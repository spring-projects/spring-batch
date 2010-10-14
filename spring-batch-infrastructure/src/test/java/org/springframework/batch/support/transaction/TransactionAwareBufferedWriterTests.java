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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Dave Syer
 * 
 */
public class TransactionAwareBufferedWriterTests {

	private Writer stringWriter = new StringWriter();

	private TransactionAwareBufferedWriter writer = new TransactionAwareBufferedWriter(stringWriter, new Runnable() {
		public void run() {
			try {
				stringWriter.append("c");
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	});

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	private boolean flushed = false;

	/**
	 * Test method for
	 * {@link org.springframework.batch.support.transaction.TransactionAwareBufferedWriter#write(java.lang.String)}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testWriteOutsideTransaction() throws Exception {
		writer.write("foo");
		writer.flush();
		// Not closed yet
		assertEquals("foo", stringWriter.toString());
	}

	@Test
	public void testBufferSizeOutsideTransaction() throws Exception {
		writer.write("foo");
		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testCloseOutsideTransaction() throws Exception {
		writer.write("foo");
		writer.close();
		assertEquals("fooc", stringWriter.toString());
	}

	@Test
	public void testFlushInTransaction() throws Exception {
		Writer mock = new Writer() {
			@Override
			public void close() throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void flush() throws IOException {
				flushed = true;
			}

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
			}
		};
		writer = new TransactionAwareBufferedWriter(mock, new Runnable() {
			public void run() {				
			}
		});
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write("foo");
					writer.flush();
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				assertFalse(flushed);
				return null;
			}
		});
		assertTrue(flushed);
	}

	@Test
	public void testWriteWithCommit() throws Exception {
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write("foo");
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				assertEquals("", stringWriter.toString());
				return null;
			}
		});
		// Not closed in transaction
		assertEquals("foo", stringWriter.toString());
	}

	@Test
	public void tesBufferSizeInTransaction() throws Exception {
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write("foo");
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				assertEquals(3, writer.getBufferSize());
				return null;
			}
		});
	}

	@Test
	public void testWriteWithRollback() throws Exception {
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					try {
						writer.write("foo");
					}
					catch (IOException e) {
						throw new IllegalStateException("Unexpected IOException", e);
					}
					assertEquals("", stringWriter.toString());
					throw new RuntimeException("Planned failure");
				}
			});
		}
		catch (RuntimeException e) {
			// expected
			String message = e.getMessage();
			assertEquals("Wrong message:  " + message, "Planned failure", message);
		}
		assertEquals("", stringWriter.toString());
	}

	@Test
	public void testCleanUpAfterRollback() throws Exception {
		testWriteWithRollback();
		testWriteWithCommit();
	}

}
