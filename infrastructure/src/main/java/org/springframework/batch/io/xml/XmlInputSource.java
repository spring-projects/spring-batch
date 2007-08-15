/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.io.xml;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XmlInputTemplate is implementation of {@link InputSource} which
 * processes XML input independently of technologies used for parsing XML files
 * and mapping XML to value objects. It has references only to interfaces, not
 * concrete implementations. It uses {@link ObjectInputFactory} interface for
 * getting {@link ObjectInput}, which is interface for retrieving value objects
 * from the input stream. So it's easy to plug-in any technology for parsing XML
 * and mapping it to value object by implementing these interfaces. See
 * implementation of {@link ObjectInputFactory} interface, which uses StAX
 * as XML parser and XStream as XML-to-ValueObjects mapper.
 * <p>
 * Current implementation also allows validation of XML file against XSD schema.
 * This validation is realized by using SAXParser. Validation of the whole XML
 * file is performed in the {@link #open()} method, because SAXParser does not
 * allow to process only part of the XML, then stop and continue later with
 * processing. This type of processing can be realized by using any pull-parser
 * (e.g. StAX), but StAX API does not provide methods for validation. So be
 * careful when using this validation, because it means to parse XML twice: once
 * to validate and once to read and map. Validation can be turned on/off by
 * {@link #setValidating(boolean)} method. By default is validation turned off.<br/>
 * This validation should be refactored in future. Validation should be provided
 * either by {@link ObjectInput} or some new interface.
 * <p>
 * This input template also provides restart, skip, statistics and transaction
 * features by implementing corresponding interfaces.
 * 
 * @author peter.zozom
 * @see ObjectInput
 * @see ObjectInputFactory
 */
public class XmlInputSource implements InputSource, Skippable, Restartable,
		TransactionSynchronization, StatisticsProvider, InitializingBean, DisposableBean {
	
	private static final Log log = LogFactory.getLog(XmlInputSource.class);

	private static final String DEFAULT_ENCODING = "UTF-8";

	/*
	 * Unique source name used to construct this xml reader input source -
	 * specified by the configuration file.
	 */
	private String sourceName = null;

	private Resource resource;

	private ObjectInputFactory inputFactory;

	private boolean validating = false;

	private String encoding = DEFAULT_ENCODING;
	
	private Properties statistics = new Properties();

	public static final String READ_STATISTICS_NAME = "Read";
	
	public static final String RESTART_DATA_NAME = "xmlinputtemplate.currentRecordCount";

	private InputState state = new InputState();

	// accessor method for the threadlocal state object
	private InputState getState() {
		return (InputState) state;
	}
	
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.state(resource.exists(), "Input resource does not exist: ["+resource+"]");
	}

	/**
	 * Setter for input resource.
	 * 
	 * @param resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}
	
	/**
	 * Return current status of the validation flag
	 * 
	 * @return
	 */
	public boolean isValidating() {
		return validating;
	}

	/**
	 * Turn validation on/off for the input source
	 * 
	 * @param validating
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	/**
	 * Initialize the input source and validates input XML file if validation is
	 * turned on. This method should be called for each thread using same
	 * XmlInputTemplate instance.
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public void open() {
		InputState is = getState();
		if (!is.initialized) {
			registerSynchronization();
			initializeObjectInput();
		}
	}

	/**
	 * Registers object for transaction synchronization
	 */
	protected void registerSynchronization() {
		BatchTransactionSynchronizationManager.registerSynchronization(this);
	}

	/**
	 * Close the input source
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {
		InputState is = getState();
		is.objectInput.close();
		is.initialized = false;
	}

	/**
	 * Calls close to ensure that bean factories can close and always release
	 * resources.
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/*
	 * Initializes and obtains the ObjectInput from the ObjectInputFactory and
	 * validates input XML file if validation is turned on.
	 */
	private void initializeObjectInput() {
		InputState is = getState();
		is.initialized = false;

		if (isValidating()) {
			validateInputFile(resource);
		}

		is.objectInput = inputFactory.createObjectInput(resource, getEncoding());

		is.lastCommitPoint = is.objectInput.position();
		is.initialized = true;
	}

	/*
	 * Validates input xml file against XSD schema.
	 * 
	 * @param file File to be validated
	 */
	private void validateInputFile(Resource resource) {
		try {
			SAXParserFactory factory = getSaxFactory();

			factory.setValidating(true);
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/validation/schema", true);

			SAXParser parser = factory.newSAXParser();
			DefaultHandler handler = getDefaultHandler();
			parser.parse(resource.getInputStream(), handler, resource.getURL().toExternalForm());
		}
		catch (ParserConfigurationException pce) {
			log.error(pce);
			throw new BatchEnvironmentException("Unable to configure parser.", pce);
		}
		catch (SAXException se) {
			log.error(se);
			throw new BatchEnvironmentException("Error during parsing the input file.", se);
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new BatchEnvironmentException("Error reading input file.", ioe);
		}
	}

	/*
	 * Get default handler for xml validation
	 * 
	 * @return instance of DefaultHandler
	 */
	private DefaultHandler getDefaultHandler() {
		return new XmlErrorHandler();
	}

	/*
	 * Creates instance of SAXParserFactory
	 * 
	 * @return instance of SAXParserFactory @throws FactoryConfigurationError
	 */
	protected SAXParserFactory getSaxFactory() throws FactoryConfigurationError {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		return factory;
	}

	/**
	 * Return the next record from the input file and map it to a value object.
	 * 
	 * @return next record if found or <code>null</code> to signal the end of
	 * the input data.
	 * @see org.springframework.batch.io.InputSource
	 */
	public Object read() {
		InputState is = getState();
		
		if (!is.initialized) {
			open();
		}

		Object result = null;

		do {
			result = readNextRecord();
			is.currentRecordCount++;
		} while (is.skipLines.contains(new Integer(is.currentRecordCount)));

		return result;
	}

	/*
	 * Reads next record
	 * 
	 * @return next record
	 */
	private Object readNextRecord() {
		InputState is = getState();
		Object result;

		try {
			result = is.objectInput.readObject();
		}
		catch (EOFException eofe) {
			log.debug("Parsing of XML finished");
			result = null;
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new BatchCriticalException("Unable to read from ObjectInputStream", ioe);
		}
		catch (ClassNotFoundException cnfe) {
			log.error(cnfe);
			throw new BatchEnvironmentException("Bad xml mapping", cnfe);
		}

		return result;
	}

	/**
	 * Postprocess after transaction commit/rollback. Called from transaction
	 * manager.
	 * 
	 * @param status indicates whether it was a rollback or commit
	 */
	public void afterCompletion(int status) {
		if (status == TransactionSynchronization.STATUS_COMMITTED) {
			transactionComitted();
		}
		else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
			transactionRolledback();
		}
	}

	/*
	 * Postprocess after transaction commit
	 */
	private void transactionComitted() {
		InputState is = getState();
		is.skipLines = new ArrayList();
		is.lastCommitPoint = is.objectInput.position();
	}

	/*
	 * Postprocess after transaction rollback
	 */
	private void transactionRolledback() {
		InputState is = getState();
		is.currentRecordCount = 0;

		// remember list of skipped lines and last commit point
		List sl = is.skipLines;
		long cp = is.lastCommitPoint;
		// XMLStreamReader is forward only, so we need to start from beginnig
		close();
		// this will also reset skipLines
		open();
		// so after init get new InputState and set skipLines and
		// lastCommitPoint
		is = getState();
		is.skipLines = sl;
		is.lastCommitPoint = cp;

		long currentLocation = is.objectInput.position();
		while (currentLocation < is.lastCommitPoint) {
			readNextRecord();
			is.currentRecordCount++;
			currentLocation = is.objectInput.position();
		}
	}

	/**
	 * Returns name of the input source.
	 * 
	 * @return input source name
	 */
	public String getName() {
		return sourceName;
	}

	/**
	 * Sets name of the input source.
	 * 
	 */
	public void setName(String newName) {
		this.sourceName = newName;
	}

	/**
	 * This method returns the restart data for the input source. It returns the
	 * current record count which can be used to re-initialze the batch job in
	 * case of restart.
	 * 
	 * @see org.springframework.batch.container.Restartable#getRestartData()
	 */
	public RestartData getRestartData() {
		Properties restartData = new Properties();
		restartData.setProperty(RESTART_DATA_NAME, String.valueOf(getState().currentRecordCount));
		return new GenericRestartData(restartData);
	}

	/**
	 * This method initializes the input source for restart. It opens the input
	 * file and position the xml reader according to information provided by the
	 * restart data.
	 * 
	 * @param restartData restart data information
	 * @see org.springframework.batch.container.Restartable#initForRestart(java.lang.Object)
	 */
	public void restoreFrom(RestartData restartData) {
		if (restartData == null || restartData.getProperties() == null || 
				restartData.getProperties().getProperty(RESTART_DATA_NAME) == null) {
			return;
		}
		
		InputState is = getState();
		int startAtRecord = Integer.parseInt(restartData.getProperties().getProperty(RESTART_DATA_NAME));

		for (int i = 0; i < startAtRecord; i++) {
			readNextRecord();
		}

		is.currentRecordCount = startAtRecord;
	}

	/**
	 * Skip the current record.
	 * @see org.springframework.batch.container.advice.SkipAdvice#skip()
	 */
	public void skip() {
		InputState is = getState();
		is.skipLines.add(new Integer(is.currentRecordCount));
	}

	/**
	 * Get encoding.
	 * @return the character encoding of the stream
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Set encoding.
	 * @param encoding the character encoding of the stream
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get statistics for the processed input.
	 * @return actual statistics for the processed input
	 * @see org.springframework.batch.container.advice.StatisticsAdvice#getStatistics()
	 */
	public Properties getStatistics() {
		
		statistics.setProperty(READ_STATISTICS_NAME, String.valueOf(getState().currentRecordCount));
		return statistics;
	}

	/**
	 * Set the ObjectInputFactory which is used for retrieving ObjectInput
	 * @param inputFactory the factory to use
	 */
	public void setInputFactory(ObjectInputFactory inputFactory) {
		this.inputFactory = inputFactory;
	}

	/* *** Intentionally unimplemented methods *** */

	public void suspend() {
	}

	public void resume() {
	}

	public void beforeCommit(boolean arg0) {
	}

	public void beforeCompletion() {
	}

	public void afterCommit() {
	}

	/**
	 * Value object holding state of the input source.
	 */
	private static class InputState {
		boolean initialized = false;

		int currentRecordCount = 0;

		ObjectInput objectInput;

		List skipLines = new ArrayList();

		long lastCommitPoint;
	}
}
