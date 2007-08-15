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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.OutputSource;
import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;

/**
 * XmlOutputTemplate is implementation of {@link OutputSource} which processes
 * XML output independently of technologies used for serializing value objects
 * to XML files. It has references only to interfaces, not concrete
 * implementations. It uses {@link ObjectOutputFactory} interface for getting
 * {@link ObjectOutput}, which is interface for writing value objects to the
 * output stream. So it's easy to plug-in any technology for serializing value
 * objects to XML files by implementing these interfaces. See implementations of
 * {@link ObjectOutputFactory} interface.
 * <p>
 * This output template also provides restart, statistics and transaction
 * features by implementing corresponding 'advice' interfaces.
 * 
 * @author peter.zozom
 * @see ObjectOutput
 * @see ObjectOutputFactory
 */
public class XmlOutputSource implements OutputSource, Restartable, StatisticsProvider, TransactionSynchronization,
		DisposableBean {
	private static final Log log = LogFactory.getLog(XmlOutputSource.class);

	private static final String DEFAULT_ENCODING = "UTF-8";

	private ObjectOutputFactory outputFactory;

	private String encoding = DEFAULT_ENCODING;

	private Resource resource;

	private Properties statistics = new Properties();

	/*
	 * Unique source name used to construct this xml reader input source -
	 * specified by the configuration file.
	 */
	private String sourceName = null;

	public static final String WRITTEN_STATISTICS_NAME = "Written";

	public static final String RESTART_DATA_NAME = "xmloutputtemplate.currentRecordCount";

	private OutputState state;

	// Accessor method for the state object
	private OutputState getState() {
		if (state == null) {
			state = new OutputState();
		}
		return state;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.state(resource.getFile().canWrite(), "Resource is not writable");
	}

	/**
	 * Setter for resource. Represents a file that can be written.
	 * 
	 * @param resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Initialize the output source. This method should be called for each
	 * thread using same XmlOutputTemplate instance.
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public void open() {

		OutputState os = getState();
		if (!os.initialized) {
		}
	}

	/**
	 * Registers object for transaction synchronization
	 */
	protected void registerSynchronization() {
		BatchTransactionSynchronizationManager.registerSynchronization(this);
	}

	/**
	 * Just calls {@link #close()} so that bean factories will clean up
	 * resources correctly.
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/**
	 * Close the output source
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {
		OutputState os = getState();
		try {
			if (os != null && os.objectOutput != null) {
				os.objectOutput.close();
			}
		}
		finally {
			if (os != null) {
				os.initialized = false;
				os.restarted = false;
			}
		}
	}

	/*
	 * Initializes and obtains a ObjectOutput from ObjectOutputFactory.
	 */
	private void initializeXmlWriter() {
		OutputState os = getState();
		os.initialized = false;
		File file;

		try {
			file = resource.getFile();

			// If the output source was restarted, keep existing file.
			// If the output source was not restarted, check following:
			// - if the file should be deleted, delete it if it was exiting and
			// create blank file,
			// - if the file should not be deleted, if it already exists, throw
			// an exception,
			// - if the file was not existing, create new.
			if (!os.restarted) {
				if (file.exists()) {
					if (os.shouldDeleteIfExists) {
						file.delete();
					}
					else {
						throw new BatchEnvironmentException("Resource already exists: " + resource);
					}
				}
				file.createNewFile();
			}

		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]", ioe);
		}

		os.objectOutput = outputFactory.createObjectOutput(resource, encoding);

		if (os.restarted) {
			os.objectOutput.afterRestart(new Long(os.lastMarkedByteOffsetPosition));
			checkFileSize();
		}

		os.initialized = true;
	}

	/**
	 * Write the value object to output xml stream.
	 * @param output the value object
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) {
		OutputState os = getState();
		if (!os.initialized) {
			open();
			initializeXmlWriter();
		}

		try {
			os.objectOutput.writeObject(output);
			os.objectsWritten++;
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to write to ObjectOutputStream", ioe);
		}
	}

	/**
	 * Return the name of the output source.
	 */
	public void setName(String newName) {
		this.sourceName = newName;
	}

	/**
	 * Set the name of the output source.
	 * 
	 * @see org.springframework.batch.restart.Restartable#getName()
	 */
	public String getName() {
		return sourceName;
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
		OutputState os = getState();

		os.objectOutput.flush();
		os.lastMarkedByteOffsetPosition = this.position();
	}

	/*
	 * Postprocess after transaction rollback
	 */
	private void transactionRolledback() {
		checkFileSize();
		resetPositionForRestart();
	}

	/*
	 * This method removes any information in the file before this reset point.
	 * 
	 * @param resetByteOffset a long integer representing the byte offset to
	 * trunction and reposition the file cursor to for restarting.
	 */
	private void resetPositionForRestart() {
		OutputState os = getState();
		os.objectOutput.truncate(os.lastMarkedByteOffsetPosition);
		os.objectOutput.position(os.lastMarkedByteOffsetPosition);
	}

	/*
	 * Checks (on setState) to make sure that the current output file's size is
	 * not smaller than the last saved commit point. If it is, then the file has
	 * been damaged in some way and whole task must be started over again from
	 * the beginning.
	 */
	private void checkFileSize() {
		OutputState os = getState();
		long size = -1;

		size = os.objectOutput.size();

		Assert.state(size >= os.lastMarkedByteOffsetPosition, "Current file size is smaller than size at last commit");
	}

	/*
	 * Return the byte offset position of the cursor in the output file as a
	 * long integer.
	 * 
	 * @return long integer representing the byte offset position of the cursor
	 * in the output file.
	 */
	private long position() {
		OutputState os = getState();
		return os.objectOutput.position();
	}

	/**
	 * Get statistics for the processed output.
	 * @return actual statistics for the processed output
	 * @see org.springframework.batch.container.advice.StatisticsAdvice#getStatistics()
	 */
	public Properties getStatistics() {

		statistics.setProperty(WRITTEN_STATISTICS_NAME, new Long(getState().objectsWritten).toString());
		return statistics;
	}

	/**
	 * Set encoding.
	 * @param encoding the character encoding of the stream
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set the ObjectOutputFactory which is used for retrieving ObjectOutput
	 * @param outputFactory the factory to use
	 */
	public void setOutputFactory(ObjectOutputFactory outputFactory) {
		this.outputFactory = outputFactory;
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

	/*
	 * Value object holding state of the output source.
	 */
	private static class OutputState {
		private boolean initialized = false;

		private ObjectOutput objectOutput;

		private long lastMarkedByteOffsetPosition = 0;

		private long objectsWritten = 0;

		private boolean shouldDeleteIfExists = true;

		private boolean restarted = false;

	}

	public void restoreFrom(RestartData data) {
		if (data == null || data.getProperties() == null || 
				data.getProperties().getProperty(RESTART_DATA_NAME) == null) {
			return;
		}
		
		OutputState os = getState();
		os.lastMarkedByteOffsetPosition = Long.parseLong(data.getProperties().getProperty(RESTART_DATA_NAME));
		os.restarted = true;
		initializeXmlWriter();
	}

	/**
	 * This method returns the restart data for the output source. It returns
	 * the current byte offset position of the cursor in the output file which
	 * can be used to re-initialze the batch job in case of restart.
	 * 
	 * @see org.springframework.batch.container.Restartable#getRestartData()
	 */

	public RestartData getRestartData() {
		Properties restartData = new Properties();
		restartData.setProperty(RESTART_DATA_NAME, new Long(position()).toString());
		return new GenericRestartData(restartData);
	}

	/**
	 * This method initializes the output source for restart. It opens the
	 * output file and position the xml writer according to information provided
	 * by the restart data.
	 * 
	 * @param restartData restart data information
	 * @see org.springframework.batch.container.advice.RestartAdvice#initForRestart(java.lang.Object)
	 */
	public void initForRestart(Object restartData) {

	}
}
