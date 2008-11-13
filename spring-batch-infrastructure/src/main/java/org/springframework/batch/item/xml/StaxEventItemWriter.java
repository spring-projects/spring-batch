package org.springframework.batch.item.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.batch.item.util.FileUtils;
import org.springframework.batch.item.xml.stax.NoStartEndDocumentStreamWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * An implementation of {@link ItemWriter} which uses StAX and
 * {@link EventWriterSerializer} for serializing object to XML.
 * 
 * This item writer also provides restart, statistics and transaction features
 * by implementing corresponding interfaces.
 * 
 * Output is buffered until {@link #flush()} is called - only then the actual
 * writing to file takes place.
 * 
 * The implementation is *not* thread-safe.
 * 
 * @author Peter Zozom
 * @author Robert Kasanicky
 * 
 */
public class StaxEventItemWriter extends ExecutionContextUserSupport implements ItemWriter, ItemStream,
		InitializingBean {

	private static final Log log = LogFactory.getLog(StaxEventItemWriter.class);

	// default encoding
	private static final String DEFAULT_ENCODING = "UTF-8";

	// default encoding
	private static final String DEFAULT_XML_VERSION = "1.0";

	// default root tag name
	private static final String DEFAULT_ROOT_TAG_NAME = "root";

	// restart data property name
	private static final String RESTART_DATA_NAME = "position";

	// restart data property name
	private static final String WRITE_STATISTICS_NAME = "record.count";

	// file system resource
	private Resource resource;

	// xml serializer
	private EventWriterSerializer serializer;

	// encoding to be used while reading from the resource
	private String encoding = DEFAULT_ENCODING;

	// XML version
	private String version = DEFAULT_XML_VERSION;

	// name of the root tag
	private String rootTagName = DEFAULT_ROOT_TAG_NAME;

	// root element attributes
	private Map rootElementAttributes = null;

	// TRUE means, that output file will be overwritten if exists - default is
	// TRUE
	private boolean overwriteOutput = true;

	// file channel
	private FileChannel channel;

	// wrapper for XML event writer that swallows StartDocument and EndDocument
	// events
	private XMLEventWriter eventWriter;

	// XML event writer
	private XMLEventWriter delegateEventWriter;

	// byte offset in file channel at last commit point
	private long lastCommitPointPosition = 0;

	// processed record count at last commit point
	private long lastCommitPointRecordCount = 0;

	// current count of processed records
	private long currentRecordCount = 0;

	private boolean saveState = true;

	// holds the list of items for writing before they are actually written on
	// #flush()
	private List buffer = new ArrayList();

	private List headers = new ArrayList();

	public StaxEventItemWriter() {
		setName(ClassUtils.getShortName(StaxEventItemWriter.class));
	}

	/**
	 * Set output file.
	 * 
	 * @param resource the output file
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Set Object to XML serializer.
	 * 
	 * @param serializer the Object to XML serializer
	 */
	public void setSerializer(EventWriterSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Get used encoding.
	 * 
	 * @return the encoding used
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Set encoding to be used for output file.
	 * 
	 * @param encoding the encoding to be used
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get XML version.
	 * 
	 * @return the XML version used
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set XML version to be used for output XML.
	 * 
	 * @param version the XML version to be used
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Get the tag name of the root element.
	 * 
	 * @return the root element tag name
	 */
	public String getRootTagName() {
		return rootTagName;
	}

	/**
	 * Set the tag name of the root element. If not set, default name is used
	 * ("root").
	 * 
	 * @param rootTagName the tag name to be used for the root element
	 */
	public void setRootTagName(String rootTagName) {
		this.rootTagName = rootTagName;
	}

	/**
	 * Get attributes of the root element.
	 * 
	 * @return attributes of the root element
	 */
	public Map getRootElementAttributes() {
		return rootElementAttributes;
	}

	/**
	 * Set the root element attributes to be written.
	 * 
	 * @param rootElementAttributes attributes of the root element
	 */
	public void setRootElementAttributes(Map rootElementAttributes) {
		this.rootElementAttributes = rootElementAttributes;
	}

	/**
	 * Set "overwrite" flag for the output file. Flag is ignored when output
	 * file processing is restarted.
	 * 
	 * @param overwriteOutput
	 */
	public void setOverwriteOutput(boolean overwriteOutput) {
		this.overwriteOutput = overwriteOutput;
	}

	/**
	 * Setter for the headers. This list will be marshalled and output before
	 * any calls to {@link #write(Object)}.
	 * @param headers
	 */
	public void setHeaderItems(Object[] headers) {
		this.headers = Arrays.asList(headers);
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * @throws Exception
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(serializer);
	}

	/**
	 * Open the output source
	 * 
	 * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
	 */
	public void open(ExecutionContext executionContext) {
		
		Assert.notNull(resource);
		
		long startAtPosition = 0;
		boolean restarted = false;

		// if restart data is provided, restart from provided offset
		// otherwise start from beginning
		if (executionContext.containsKey(getKey(RESTART_DATA_NAME))) {
			startAtPosition = executionContext.getLong(getKey(RESTART_DATA_NAME));
			restarted = true;
		}

		open(startAtPosition, restarted);

		if (startAtPosition == 0) {
			for (Iterator iterator = headers.iterator(); iterator.hasNext();) {
				Object header = (Object) iterator.next();
				write(header);
			}
		}
	}

	/**
	 * Helper method for opening output source at given file position
	 */
	private void open(long position, boolean restarted) {

		File file;
		FileOutputStream os = null;

		try {
			file = resource.getFile();
			FileUtils.setUpOutputFile(file, restarted, overwriteOutput);
			Assert.state(resource.exists(), "Output resource must exist");
			os = new FileOutputStream(file, true);
			channel = os.getChannel();
			setPosition(position);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", ioe);
		}

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

		try {
			delegateEventWriter = outputFactory.createXMLEventWriter(os, encoding);
			eventWriter = new NoStartEndDocumentStreamWriter(delegateEventWriter);
			if (!restarted) {
				startDocument(delegateEventWriter);
			}
		}
		catch (XMLStreamException xse) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", xse);
		}

	}

	/**
	 * Writes simple XML header containing:
	 * <ul>
	 * <li>xml declaration - defines encoding and XML version</li>
	 * <li>opening tag of the root element and its attributes</li>
	 * </ul>
	 * If this is not sufficient for you, simply override this method. Encoding,
	 * version and root tag name can be retrieved with corresponding getters.
	 * 
	 * @param writer XML event writer
	 * @throws XMLStreamException
	 */
	private void startDocument(XMLEventWriter writer) throws XMLStreamException {

		XMLEventFactory factory = XMLEventFactory.newInstance();

		// write start document
		writer.add(factory.createStartDocument(getEncoding(), getVersion()));

		// write root tag
		writer.add(factory.createStartElement("", "", getRootTagName()));

		// write root tag attributes
		if (!CollectionUtils.isEmpty(getRootElementAttributes())) {

			for (Iterator i = getRootElementAttributes().entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				writer.add(factory.createAttribute((String) entry.getKey(), (String) entry.getValue()));
			}

		}

	}

	/**
	 * Writes the EndDocument tag manually.
	 * 
	 * @param writer XML event writer
	 * @throws XMLStreamException
	 */
	protected void endDocument(XMLEventWriter writer) throws XMLStreamException {

		// writer.writeEndDocument(); <- this doesn't work after restart
		// we need to write end tag of the root element manually

		ByteBuffer bbuf = ByteBuffer.wrap(("</" + getRootTagName() + ">").getBytes());
		try {
			channel.write(bbuf);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to close file resource: [" + resource + "]", ioe);
		}
	}

	/**
	 * Flush and close the output source.
	 * 
	 * @see org.springframework.batch.item.ItemStream#close(ExecutionContext)
	 */
	public void close(ExecutionContext executionContext) {
		
		// harmless event to close the root tag if there were no items
		XMLEventFactory factory = XMLEventFactory.newInstance();
		try {
			delegateEventWriter.add(factory.createCharacters(""));
		}
		catch (XMLStreamException e) {
			log.error(e);
		}

		flush();
		try {
			endDocument(delegateEventWriter);
			eventWriter.close();
			channel.close();
		}
		catch (XMLStreamException xse) {
			throw new DataAccessResourceFailureException("Unable to close file resource: [" + resource + "]", xse);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to close file resource: [" + resource + "]", ioe);
		}
	}

	/**
	 * Write the value object to internal buffer.
	 * 
	 * @param item the value object
	 * @see #flush()
	 */
	public void write(Object item) {

		currentRecordCount++;
		buffer.add(item);
	}

	/**
	 * Get the restart data.
	 * 
	 * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
	 */
	public void update(ExecutionContext executionContext) {

		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putLong(getKey(RESTART_DATA_NAME), getPosition());
			executionContext.putLong(getKey(WRITE_STATISTICS_NAME), currentRecordCount);
		}
	}

	/*
	 * Get the actual position in file channel. This method flushes any buffered
	 * data before position is read.
	 * 
	 * @return byte offset in file channel
	 */
	private long getPosition() {

		long position;

		try {
			eventWriter.flush();
			position = channel.position();
		}
		catch (Exception e) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", e);
		}

		return position;
	}

	/**
	 * Set the file channel position.
	 * 
	 * @param newPosition new file channel position
	 */
	private void setPosition(long newPosition) {

		try {
			Assert.state(channel.size() >= lastCommitPointPosition,
					"Current file size is smaller than size at last commit");
			channel.truncate(newPosition);
			channel.position(newPosition);
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", e);
		}

	}

	/**
	 * Writes buffered items to XML stream and marks restore point.
	 */
	public void flush() throws FlushFailedException {

		for (Iterator iterator = buffer.listIterator(); iterator.hasNext();) {
			Object item = iterator.next();
			serializer.serializeObject(eventWriter, item);
		}
		try {
			eventWriter.flush();
		}
		catch (XMLStreamException e) {
			throw new FlushFailedException("Failed to flush the events", e);
		}
		buffer.clear();

		lastCommitPointPosition = getPosition();
		lastCommitPointRecordCount = currentRecordCount;
	}

	/**
	 * Clear the output buffer
	 */
	public void clear() throws ClearFailedException {
		currentRecordCount = lastCommitPointRecordCount;
		buffer.clear();
	}

}
