/*
 * Copyright 2006-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.batch.item.util.FileUtils;
import org.springframework.batch.item.xml.stax.NoStartEndDocumentStreamWriter;
import org.springframework.batch.item.xml.stax.UnclosedElementCollectingEventWriter;
import org.springframework.batch.item.xml.stax.UnopenedElementClosingEventWriter;
import org.springframework.batch.support.transaction.TransactionAwareBufferedWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * An implementation of {@link ItemWriter} which uses StAX and
 * {@link Marshaller} for serializing object to XML.
 * 
 * This item writer also provides restart, statistics and transaction features
 * by implementing corresponding interfaces.
 * 
 * The implementation is <b>not</b> thread-safe.
 * 
 * @author Peter Zozom
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
public class StaxEventItemWriter<T> extends AbstractItemStreamItemWriter<T> implements
ResourceAwareItemWriterItemStream<T>, InitializingBean {

	private static final Log log = LogFactory.getLog(StaxEventItemWriter.class);

	// default encoding
	public static final String DEFAULT_ENCODING = "UTF-8";

	// default encoding
	public static final String DEFAULT_XML_VERSION = "1.0";

	// default standalone document declaration, value not set
	public static final Boolean DEFAULT_STANDALONE_DOCUMENT = null;

	// default root tag name
	public static final String DEFAULT_ROOT_TAG_NAME = "root";

	// restart data property name
	private static final String RESTART_DATA_NAME = "position";

	// unclosed header callback elements property name
	private static final String UNCLOSED_HEADER_CALLBACK_ELEMENTS_NAME = "unclosedHeaderCallbackElements";
	
	// restart data property name
	private static final String WRITE_STATISTICS_NAME = "record.count";

	// file system resource
	private Resource resource;

	// xml marshaller
	private Marshaller marshaller;

	// encoding to be used while reading from the resource
	private String encoding = DEFAULT_ENCODING;

	// XML version
	private String version = DEFAULT_XML_VERSION;

	// standalone header attribute
	private Boolean standalone = DEFAULT_STANDALONE_DOCUMENT;

	// name of the root tag
	private String rootTagName = DEFAULT_ROOT_TAG_NAME;

	// namespace prefix of the root tag
	private String rootTagNamespacePrefix = "";

	// namespace of the root tag
	private String rootTagNamespace = "";

	// root element attributes
	private Map<String, String> rootElementAttributes = null;

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

	// current count of processed records
	private long currentRecordCount = 0;

	private boolean saveState = true;

	private StaxWriterCallback headerCallback;

	private StaxWriterCallback footerCallback;

	private Writer bufferedWriter;

	private boolean transactional = true;

	private boolean forceSync;

	private boolean shouldDeleteIfEmpty = false;
	
	private boolean restarted = false;

	private boolean initialized = false;
	
	// List holding the QName of elements that were opened in the header callback, but not closed
	private List<QName> unclosedHeaderCallbackElements = Collections.emptyList();

	public StaxEventItemWriter() {
		setExecutionContextName(ClassUtils.getShortName(StaxEventItemWriter.class));
	}

	/**
	 * Set output file.
	 * 
	 * @param resource the output file
	 */
	@Override
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Set Object to XML marshaller.
	 * 
	 * @param marshaller the Object to XML marshaller
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * headerCallback is called before writing any items.
	 *
	 * @param headerCallback the {@link StaxWriterCallback} to be called prior to writing items.
	 */
	public void setHeaderCallback(StaxWriterCallback headerCallback) {
		this.headerCallback = headerCallback;
	}

	/**
	 * footerCallback is called after writing all items but before closing the
	 * file.
	 *
	 *@param footerCallback the {@link StaxWriterCallback} to be called after writing items.
	 */
	public void setFooterCallback(StaxWriterCallback footerCallback) {
		this.footerCallback = footerCallback;
	}

	/**
	 * Flag to indicate that writes should be deferred to the end of a
	 * transaction if present. Defaults to true.
	 * 
	 * @param transactional the flag to set
	 */
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	/**
	 * Flag to indicate that changes should be force-synced to disk on flush.
	 * Defaults to false, which means that even with a local disk changes could
	 * be lost if the OS crashes in between a write and a cache flush. Setting
	 * to true may result in slower performance for usage patterns involving
	 * many frequent writes.
	 * 
	 * @param forceSync the flag value to set
	 */
	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	/**
	 * Flag to indicate that the target file should be deleted if no items have
	 * been written (other than header and footer) on close. Defaults to false.
	 * 
	 * @param shouldDeleteIfEmpty the flag value to set
	 */
	public void setShouldDeleteIfEmpty(boolean shouldDeleteIfEmpty) {
		this.shouldDeleteIfEmpty = shouldDeleteIfEmpty;
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
	 * Get used standalone document declaration.
	 *
	 * @return the standalone document declaration used
	 *
	 * @since 4.3
	 */
	public Boolean getStandalone() {
		return standalone;
	}

	/**
	 * Set standalone document declaration to be used for output XML. If not set,
	 * standalone document declaration will be omitted.
	 *
	 * @param standalone the XML standalone document declaration to be used
	 *
	 * @since 4.3
	 */
	public void setStandalone(Boolean standalone) {
		this.standalone = standalone;
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
	 * ("root"). Namespace URI and prefix can also be set optionally using the
	 * notation:
	 * 
	 * <pre>
	 * {uri}prefix:root
	 * </pre>
	 * 
	 * The prefix is optional (defaults to empty), but if it is specified then
	 * the uri must be provided. In addition you might want to declare other
	 * namespaces using the {@link #setRootElementAttributes(Map) root
	 * attributes}.
	 * 
	 * @param rootTagName the tag name to be used for the root element
	 */
	public void setRootTagName(String rootTagName) {
		this.rootTagName = rootTagName;
	}

	/**
	 * Get the namespace prefix of the root element. Empty by default.
	 * 
	 * @return the rootTagNamespacePrefix
	 */
	public String getRootTagNamespacePrefix() {
		return rootTagNamespacePrefix;
	}

	/**
	 * Get the namespace of the root element.
	 * 
	 * @return the rootTagNamespace
	 */
	public String getRootTagNamespace() {
		return rootTagNamespace;
	}

	/**
	 * Get attributes of the root element.
	 * 
	 * @return attributes of the root element
	 */
	public Map<String, String> getRootElementAttributes() {
		return rootElementAttributes;
	}

	/**
	 * Set the root element attributes to be written. If any of the key names
	 * begin with "xmlns:" then they are treated as namespace declarations.
	 * 
	 * @param rootElementAttributes attributes of the root element
	 */
	public void setRootElementAttributes(Map<String, String> rootElementAttributes) {
		this.rootElementAttributes = rootElementAttributes;
	}

	/**
	 * Set "overwrite" flag for the output file. Flag is ignored when output
	 * file processing is restarted.
	 * 
	 * @param overwriteOutput If set to true, output file will be overwritten
	 * (this flag is ignored when processing is restart).
	 */
	public void setOverwriteOutput(boolean overwriteOutput) {
		this.overwriteOutput = overwriteOutput;
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * @throws Exception thrown if error occurs
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(marshaller, "A Marshaller is required");
		if (rootTagName.contains("{")) {
			rootTagNamespace = rootTagName.replaceAll("\\{(.*)\\}.*", "$1");
			rootTagName = rootTagName.replaceAll("\\{.*\\}(.*)", "$1");
			if (rootTagName.contains(":")) {
				rootTagNamespacePrefix = rootTagName.replaceAll("(.*):.*", "$1");
				rootTagName = rootTagName.replaceAll(".*:(.*)", "$1");
			}
		}
	}

	/**
	 * Open the output source
	 *
	 * @param executionContext the batch context.
	 * 
	 * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void open(ExecutionContext executionContext) {
		super.open(executionContext);

		Assert.notNull(resource, "The resource must be set");

		long startAtPosition = 0;
		
		// if restart data is provided, restart from provided offset
		// otherwise start from beginning
		if (executionContext.containsKey(getExecutionContextKey(RESTART_DATA_NAME))) {
			startAtPosition = executionContext.getLong(getExecutionContextKey(RESTART_DATA_NAME));
			currentRecordCount = executionContext.getLong(getExecutionContextKey(WRITE_STATISTICS_NAME));
			if (executionContext.containsKey(getExecutionContextKey(UNCLOSED_HEADER_CALLBACK_ELEMENTS_NAME))) {
				unclosedHeaderCallbackElements = (List<QName>) executionContext
						.get(getExecutionContextKey(UNCLOSED_HEADER_CALLBACK_ELEMENTS_NAME));
			}
			
			restarted = true;
			if (shouldDeleteIfEmpty && currentRecordCount == 0) {
				// previous execution deleted the output file because no items were written
				restarted = false;
				startAtPosition = 0;
			} else {
				restarted = true;
			}
		} else {
			currentRecordCount = 0;
			restarted = false;
		}

		open(startAtPosition);

		if (startAtPosition == 0) {
			try {
				if (headerCallback != null) {
					UnclosedElementCollectingEventWriter headerCallbackWriter = new UnclosedElementCollectingEventWriter(delegateEventWriter);
					headerCallback.write(headerCallbackWriter);
					unclosedHeaderCallbackElements = headerCallbackWriter.getUnclosedElements();
				}
			}
			catch (IOException e) {
				throw new ItemStreamException("Failed to write headerItems", e);
			}
		}

		this.initialized = true;

	}

	/**
	 * Helper method for opening output source at given file position
	 */
	private void open(long position) {

		File file;
		FileOutputStream os = null;
		FileChannel fileChannel = null;

		try {
			file = resource.getFile();
			FileUtils.setUpOutputFile(file, restarted, false, overwriteOutput);
			Assert.state(resource.exists(), "Output resource must exist");
			os = new FileOutputStream(file, true);
			fileChannel = os.getChannel();
			channel = os.getChannel();
			setPosition(position);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", ioe);
		}

		XMLOutputFactory outputFactory = createXmlOutputFactory();

		if (outputFactory.isPropertySupported("com.ctc.wstx.automaticEndElements")) {
			// If the current XMLOutputFactory implementation is supplied by
			// Woodstox >= 3.2.9 we want to disable its
			// automatic end element feature (see:
			// https://jira.codehaus.org/browse/WSTX-165) per
			// https://jira.spring.io/browse/BATCH-761).
			outputFactory.setProperty("com.ctc.wstx.automaticEndElements", Boolean.FALSE);
		}
		if (outputFactory.isPropertySupported("com.ctc.wstx.outputValidateStructure")) {
			// On restart we don't write the root element so we have to disable
			// structural validation (see:
			// https://jira.spring.io/browse/BATCH-1681).
			outputFactory.setProperty("com.ctc.wstx.outputValidateStructure", Boolean.FALSE);
		}

		try {
			final FileChannel channel = fileChannel;
			if (transactional) {
				TransactionAwareBufferedWriter writer = new TransactionAwareBufferedWriter(channel, new Runnable() {
					@Override
					public void run() {
						closeStream();
					}
				});

				writer.setEncoding(encoding);
				writer.setForceSync(forceSync);
				bufferedWriter = writer;
			}
			else {
				bufferedWriter =  new BufferedWriter(new OutputStreamWriter(os, encoding));
			}
			delegateEventWriter = createXmlEventWriter(outputFactory, bufferedWriter);
			eventWriter = new NoStartEndDocumentStreamWriter(delegateEventWriter);
			initNamespaceContext(delegateEventWriter);
			if (!restarted) {
				startDocument(delegateEventWriter);
				if (forceSync) {
					channel.force(false);
				}
			}
		}
		catch (XMLStreamException xse) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", xse);
		}
		catch (UnsupportedEncodingException e) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource
					+ "] with encoding=[" + encoding + "]", e);
		} 
		catch (IOException e) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", e);
		}
	}

	/**
	 * Subclasses can override to customize the writer.
	 *
	 * @param outputFactory the factory to be used to create an {@link XMLEventWriter}.
	 * @param writer the {@link Writer} to be used by the {@link XMLEventWriter} for
	 * writing to character streams.
	 * @return an xml writer
	 *
	 * @throws XMLStreamException thrown if error occured creating {@link XMLEventWriter}.
	 */
	protected XMLEventWriter createXmlEventWriter(XMLOutputFactory outputFactory, Writer writer)
			throws XMLStreamException {
		return outputFactory.createXMLEventWriter(writer);
	}

	/**
	 * Subclasses can override to customize the factory.
	 *
	 * @return a factory for the xml output
	 *
	 * @throws FactoryConfigurationError throw if an instance of this factory cannot be loaded.
	 */
	protected XMLOutputFactory createXmlOutputFactory() throws FactoryConfigurationError {
		return XMLOutputFactory.newInstance();
	}

	/**
	 * Subclasses can override to customize the event factory.
	 *
	 * @return a factory for the xml events
	 *
	 * @throws FactoryConfigurationError thrown if an instance of this factory cannot be loaded.
	 */
	protected XMLEventFactory createXmlEventFactory() throws FactoryConfigurationError {
		XMLEventFactory factory = XMLEventFactory.newInstance();
		return factory;
	}

	/**
	 * Subclasses can override to customize the STAX result.
	 *
	 * @return a result for writing to
	 */
	protected Result createStaxResult() {
		return StaxUtils.createStaxResult(eventWriter);
	}

	/**
	 * Inits the namespace context of the XMLEventWriter:
	 * <ul>
	 * <li>rootTagNamespacePrefix for rootTagName</li>
	 * <li>any other xmlns namespace prefix declarations in the root element attributes</li>
	 * </ul>
	 * 
	 * @param writer XML event writer
	 *
	 * @throws XMLStreamException thrown if error occurs while setting the
	 * prefix or default name space.
	 */
	protected void initNamespaceContext(XMLEventWriter writer) throws XMLStreamException {
		if (StringUtils.hasText(getRootTagNamespace())) {
			if(StringUtils.hasText(getRootTagNamespacePrefix())) {
				writer.setPrefix(getRootTagNamespacePrefix(), getRootTagNamespace());
			} else {
				writer.setDefaultNamespace(getRootTagNamespace());
			}
		}
		if (!CollectionUtils.isEmpty(getRootElementAttributes())) {
			for (Map.Entry<String, String> entry : getRootElementAttributes().entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("xmlns")) {
					String prefix = "";
					if (key.contains(":")) {
						prefix = key.substring(key.indexOf(":") + 1);
					}
					if (log.isDebugEnabled()) {
						log.debug("registering prefix: " +prefix + "=" + entry.getValue());
					}
					writer.setPrefix(prefix, entry.getValue());
				}
			}
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
	 *
	 * @throws XMLStreamException thrown if error occurs.
	 */
	protected void startDocument(XMLEventWriter writer) throws XMLStreamException {

		XMLEventFactory factory = createXmlEventFactory();

		// write start document
		if (getStandalone() == null) {
			writer.add(factory.createStartDocument(getEncoding(), getVersion()));
		}
		else {
			writer.add(factory.createStartDocument(getEncoding(), getVersion(), getStandalone()));
		}

		// write root tag
		writer.add(factory.createStartElement(getRootTagNamespacePrefix(), getRootTagNamespace(), getRootTagName()));
		if (StringUtils.hasText(getRootTagNamespace())) {
			if (StringUtils.hasText(getRootTagNamespacePrefix())) {
				writer.add(factory.createNamespace(getRootTagNamespacePrefix(), getRootTagNamespace()));
			}
			else {
				writer.add(factory.createNamespace(getRootTagNamespace()));
			}
		}

		// write root tag attributes
		if (!CollectionUtils.isEmpty(getRootElementAttributes())) {

			for (Map.Entry<String, String> entry : getRootElementAttributes().entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("xmlns")) {
					String prefix = "";
					if (key.contains(":")) {
						prefix = key.substring(key.indexOf(":") + 1);
					}
					writer.add(factory.createNamespace(prefix, entry.getValue()));
				}
				else {
					writer.add(factory.createAttribute(key, entry.getValue()));
				}
			}

		}

		/*
		 * This forces the flush to write the end of the root element and avoids
		 * an off-by-one error on restart.
		 */
		writer.add(factory.createIgnorableSpace(""));
		writer.flush();

	}

	/**
	 * Writes the EndDocument tag manually.
	 * 
	 * @param writer XML event writer
	 *
	 * @throws XMLStreamException thrown if error occurs.
	 */
	protected void endDocument(XMLEventWriter writer) throws XMLStreamException {

		// writer.writeEndDocument(); <- this doesn't work after restart
		// we need to write end tag of the root element manually

		String nsPrefix = !StringUtils.hasText(getRootTagNamespacePrefix()) ? "" : getRootTagNamespacePrefix() + ":";
		try {
			bufferedWriter.write("</" + nsPrefix + getRootTagName() + ">");
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to close file resource: [" + resource + "]", ioe);
		}
	}

	/**
	 * Flush and close the output source.
	 * 
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	@Override
	public void close() {
		super.close();

		XMLEventFactory factory = createXmlEventFactory();
		try {
			delegateEventWriter.add(factory.createCharacters(""));
		}
		catch (XMLStreamException e) {
			log.error(e);
		}

		try {
			if (footerCallback != null) {
				XMLEventWriter footerCallbackWriter = delegateEventWriter;
				if (restarted && !unclosedHeaderCallbackElements.isEmpty()) {
					footerCallbackWriter = new UnopenedElementClosingEventWriter(
							delegateEventWriter, bufferedWriter, unclosedHeaderCallbackElements);
				} 
				footerCallback.write(footerCallbackWriter);
			}
			delegateEventWriter.flush();
			endDocument(delegateEventWriter);
		}
		catch (IOException e) {
			throw new ItemStreamException("Failed to write footer items", e);
		}
		catch (XMLStreamException e) {
			throw new ItemStreamException("Failed to write end document tag", e);
		}
		finally {

			try {
				delegateEventWriter.close();
			}
			catch (XMLStreamException e) {
				log.error("Unable to close file resource: [" + resource + "] " + e);
			}
			finally {
				try {
					bufferedWriter.close();
				}
				catch (IOException e) {
					log.error("Unable to close file resource: [" + resource + "] " + e);
				}
				finally {
					if (!transactional) {
						closeStream();
					}
				}
			}
			if (currentRecordCount == 0 && shouldDeleteIfEmpty) {
				try {
					resource.getFile().delete();
				}
				catch (IOException e) {
					throw new ItemStreamException("Failed to delete empty file on close", e);
				}
			}
		}

		this.initialized = false;
	}

	private void closeStream() {
		try {
			channel.close();
		}
		catch (IOException ioe) {
			log.error("Unable to close file resource: [" + resource + "] " + ioe);
		}
	}

	/**
	 * Write the value objects and flush them to the file.
	 * 
	 * @param items the value object
	 *
	 * @throws IOException thrown if general error occurs.
	 * @throws XmlMappingException thrown if error occurs during XML Mapping.
	 */
	@Override
	public void write(List<? extends T> items) throws XmlMappingException, IOException {

		if(!this.initialized) {
			throw new WriterNotOpenException("Writer must be open before it can be written to");
		}

		currentRecordCount += items.size();

		for (Object object : items) {
			Assert.state(marshaller.supports(object.getClass()),
					"Marshaller must support the class of the marshalled object");
			Result result = createStaxResult();
			marshaller.marshal(object, result);
		}
		try {
			eventWriter.flush();
			if (forceSync) {
				channel.force(false);
			}			
		}
		catch (XMLStreamException | IOException e) {
			throw new WriteFailedException("Failed to flush the events", e);
		} 
	}

	/**
	 * Get the restart data.
	 *
	 * @param executionContext the batch context.
	 * 
	 * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
	 */
	@Override
	public void update(ExecutionContext executionContext) {
		super.update(executionContext);
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putLong(getExecutionContextKey(RESTART_DATA_NAME), getPosition());
			executionContext.putLong(getExecutionContextKey(WRITE_STATISTICS_NAME), currentRecordCount);
			if (!unclosedHeaderCallbackElements.isEmpty()) {
				executionContext.put(getExecutionContextKey(UNCLOSED_HEADER_CALLBACK_ELEMENTS_NAME),
						unclosedHeaderCallbackElements);
			}			
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
			if (bufferedWriter instanceof TransactionAwareBufferedWriter) {
				position += ((TransactionAwareBufferedWriter) bufferedWriter).getBufferSize();
			}
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
			channel.truncate(newPosition);
			channel.position(newPosition);
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", e);
		}

	}

}
