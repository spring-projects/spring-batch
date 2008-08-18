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
package org.springframework.batch.support.transaction;

import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.item.FlushFailedException;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Wrapper for a {@link Writer} that delays actually writing to the buffer if a
 * transaction is active. If a transaction is detected on the call to
 * {@link #write(String)} the parameter is buffered and passed on to the
 * underlying writer only when the transaction is committed.
 * 
 * @author Dave Syer
 * 
 */
public class TransactionAwareBufferedWriter extends Writer {

	private static final String BUFFER_KEY = TransactionAwareBufferedWriter.class.getName() + ".BUFFER_KEY";

	private Writer writer;

	/**
	 * @param writer
	 */
	public TransactionAwareBufferedWriter(Writer writer) {
		super();
		this.writer = writer;
	}

	/**
	 * @return
	 */
	private StringBuffer getCurrentBuffer() {

		if (!TransactionSynchronizationManager.hasResource(BUFFER_KEY)) {

			TransactionSynchronizationManager.bindResource(BUFFER_KEY, new StringBuffer());

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int status) {
					StringBuffer buffer = (StringBuffer) TransactionSynchronizationManager.getResource(BUFFER_KEY);
					if (status == STATUS_COMMITTED) {
						try {
							writer.write(buffer.toString());
							writer.flush();
						}
						catch (IOException e) {
							throw new FlushFailedException("Could not write to output buffer", e);
						}
					}
					TransactionSynchronizationManager.unbindResource(BUFFER_KEY);
				}
			});

		}
		
		return (StringBuffer) TransactionSynchronizationManager.getResource(BUFFER_KEY);
		
	}

	/**
	 * @return
	 */
	private boolean transactionActive() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#close()
	 */
	@Override
	public void close() throws IOException {
		writer.close();
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#flush()
	 */
	@Override
	public void flush() throws IOException {
		if (!transactionActive()) {
			writer.flush();
		}
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {

		if (!transactionActive()) {
			writer.write(cbuf, off, len);
			return;
		}

		StringBuffer buffer = getCurrentBuffer();
		buffer.append(cbuf, off, len);
		
	}

}
