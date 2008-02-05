package org.springframework.batch.io.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.springframework.batch.io.support.FileUtils;
import org.springframework.batch.io.xml.stax.NoStartEndDocumentStreamWriter;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.StreamException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * An implementation of {@link ItemWriter} which uses StAX and
 * {@link EventWriterSerializer} for serializing object to XML.
 * 
 * This output source also provides restart, statistics and transaction features
 * by implementing corresponding interfaces.
 * 
 * @author Peter Zozom
 * 
 */
public class StaxEventItemWriter implements ItemWriter, ItemStream, InitializingBean, DisposableBean {

	// default encoding
	private static final String DEFAULT_ENCODING = "UTF-8";

	// default encoding
	private static final String DEFAULT_XML_VERSION = "1.0";

	// default root tag name
	private static final String DEFAULT_ROOT_TAG_NAME = "root";

	// restart data property name
	public static final String RESTART_DATA_NAME = "staxstreamoutputsource.position";

	// restart data property name
	public static final String WRITE_STATISTICS_NAME = "staxstreamoutputsource.record.count";

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

	// signalizes that output source has been initialized
	private boolean initialized = false;

	// signalizes that marshalling was restarted
	private boolean restarted = false;

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
	 * @param shouldDeleteIfExists
	 */
	public void setOverwriteOutput(boolean overwriteOutput) {
		this.overwriteOutput = overwriteOutput;
	}

	protected FileChannel getChannel() {
		return channel;
	}

	/**
	 * @throws Exception
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.notNull(serializer);
	}

	/**
	 * @throws Exception
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/**
	 * Open the output source
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public void open() {
		open(0);
	}

	/*
	 * Helper method for opening output source at given file position
	 */
	private void open(long position) {

		File file;
		FileOutputStream os = null;

		try {
			file = resource.getFile();
			FileUtils.setUpOutputFile(file, restarted, overwriteOutput);
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

		initialized = true;
		
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
	protected void startDocument(XMLEventWriter writer) throws XMLStreamException {

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
	 * Finishes the XML document. It closes any start tag and writes
	 * corresponding end tags.
	 * 
	 * @param writer XML event writer
	 * @throws XMLStreamException
	 */
	protected void endDocument(XMLEventWriter writer) throws XMLStreamException {

		// writer.writeEndDocument(); <- this doesn't work after restart
		// we need to write end tag of the root element manually
		writer.flush();
		ByteBuffer bbuf = ByteBuffer.wrap(("</" + getRootTagName() + ">").getBytes());
		try {
			getChannel().write(bbuf);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to close file resource: [" + resource + "]", ioe);
		}
	}

	/**
	 * Close the output source.
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {
		initialized = false;
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
	 * Write the value object to XML stream.
	 * 
	 * @param output the value object
	 * @see org.springframework.batch.item.ItemWriter#write(java.lang.Object)
	 */
	public void write(Object output) {

		if (!initialized) {
			open();
		}

		currentRecordCount++;
		serializer.serializeObject(eventWriter, output);
	}

	/**
	 * Get the restart data.
	 * @return the restart data
	 * @see org.springframework.batch.item.ItemStream#getExecutionAttributes()
	 */
	public ExecutionAttributes getExecutionAttributes() {
		if (!initialized) {
			throw new StreamException("ItemStream is not open, or may have been closed.  Cannot access context.");
		}
		ExecutionAttributes context = new ExecutionAttributes();
		context.putLong(RESTART_DATA_NAME, getPosition());
		context.putLong(WRITE_STATISTICS_NAME, currentRecordCount);
		return context;
	}

	/**
	 * Restore processing from provided restart data.
	 * @param data the restart data
	 * @see org.springframework.batch.item.ItemStream#restoreFrom(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void restoreFrom(ExecutionAttributes data) {

		long startAtPosition = 0;

		// if restart data is provided, restart from provided offset
		// otherwise start from beginning
		if (data != null && data.getProperties() != null && data.containsKey(RESTART_DATA_NAME)) {
			startAtPosition = data.getLong(RESTART_DATA_NAME);
			restarted = true;
		}

		if (!initialized) {
			open(startAtPosition);
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

	/*
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
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemStream#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void mark() {
		lastCommitPointPosition = getPosition();
		lastCommitPointRecordCount = currentRecordCount;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void reset() {
		currentRecordCount = lastCommitPointRecordCount;
		// close output
		close();
		// and reopen it - we do this because we need to reopen stream
		// reader at specified position - calling setPosition() is not
		// enough!
		restarted = true;
		open(lastCommitPointPosition);
	}

}
