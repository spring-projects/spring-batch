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

package org.springframework.batch.io.file;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.io.file.separator.LineReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.StreamException;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * <p>
 * This class is a {@link FieldSetItemReader} that supports restart,
 * skipping invalid lines and storing statistics.
 * </p>
 *
 * @author Waseem Malik
 * @author Tomas Slanina
 * @author Robert Kasanicky
 */
public class DefaultFlatFileItemReader extends SimpleFlatFileItemReader implements Skippable, ItemStream {

	private static Log log = LogFactory.getLog(DefaultFlatFileItemReader.class);

	public static final String READ_STATISTICS_NAME = "lines.read.count";

	public static final String SKIPPED_STATISTICS_NAME = "skipped.lines.count";

	private Set skippedLines = new HashSet();

	private TransactionSynchronization transactionSynchronization = new ResourceLineReaderTransactionSynchronization();

	/**
	 * Initialize the input source. 
	 */
	public void open() {
		registerSynchronization();
		super.open();
	}

	// Registers transaction synchronization.
	private void registerSynchronization() {
		BatchTransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
	}

	/**
	 * This method initialises the Input Source for Restart. It opens the input
	 * file and position the buffer reader according to information provided by
	 * the restart data
	 *
	 * @param data {@link StreamContext} information
	 */
	public void restoreFrom(StreamContext data) {

		if (data==null ||
			data.getProperties() == null ||
			data.getProperties().getProperty(READ_STATISTICS_NAME) == null ||
			getReader()==null) {
			// do nothing
			return;
		}
		log.debug("Initializing for restart. Restart data is: " + data);

		int lineCount = Integer.parseInt(data.getProperties().getProperty(READ_STATISTICS_NAME));

		LineReader reader = getReader();

		Object record = "";
		while (reader.getCurrentLineCount() < lineCount && record != null) {
			record = readLine();
		}

	}

	/**
	 * This method returns the restart Data for the input Source. It returns the
	 * current Line Count which can be used to re initialise the batch job in
	 * case of restart.
	 */
	public StreamContext getStreamContext() {
		if (reader==null) {
			throw new StreamException("ItemStream not open or already closed.");
		}
		StreamContext streamContext = new StreamContext();
		streamContext.putLong(READ_STATISTICS_NAME, reader.getCurrentLineCount());
		streamContext.putLong(SKIPPED_STATISTICS_NAME, skippedLines.size());
		return streamContext;
	}

	/**
	 * This method marks the start of a transaction. It marks the InputBuffer
	 * Reader, so that in case of rollback it can position the file to start of
	 * the transaction.
	 */
	private void transactionStarted() {
		getReader().mark();
	}

	/**
	 * Commit the transaction. At each commit point we clear the lines which we
	 * had skipped during the commit interval.
	 */
	private void transactionComitted() {
		transactionStarted();
	}

	/**
	 * Rollback the transaction.
	 */
	private void transactionRolledback() {
		getReader().reset();
	}

	/**
	 * Skip the current line which is being processed.
	 */
	public void skip() {
		Integer count = new Integer(getReader().getCurrentLineCount());
		// we are not really thread safe so we don't need to synchronize
		skippedLines.add(count);
		log.debug("Skipping line in template=[" + this + "], line=" + count);
	}

	protected String readLine() {
		String line = super.readLine();
		while (line != null && skippedLines.contains(new Integer(getReader().getCurrentLineCount()))) {
			line = super.readLine();
		}
		return line;
	}

	// added package visibility method so that tests can invoke transaction
	// events
	TransactionSynchronization getTransactionSynchronization() {
		return this.transactionSynchronization;
	}

	/**
	 * Encapsulates transaction events.
	 */
	private class ResourceLineReaderTransactionSynchronization extends TransactionSynchronizationAdapter {
		/**
		 * TransactionSynchronization method indicating that a transaction has
		 * completed.
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
	}
}
