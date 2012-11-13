/*
 * Copyright 2006-2012 the original author or authors.
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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Dave Syer
 * @author Michael Minella
 * 
 */
public class TransactionAwareBufferedWriterTests {

	private Writer stringWriter = new StringWriter();
	
	private FileChannel fileChannel;

	private TransactionAwareBufferedWriter writer;
	
	@Before
	public void init() {
		fileChannel = createMock(FileChannel.class);
		
		writer = new TransactionAwareBufferedWriter(fileChannel, new Runnable() {
			public void run() {
				try {
					ByteBuffer bb = ByteBuffer.wrap("c".getBytes());
					fileChannel.write(bb);
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});
	}

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
		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(bb))).andReturn(3);
		fileChannel.force(false);
		replay(fileChannel);

		writer.write("foo");
		writer.flush();
		// Not closed yet
		
		String s = getStringFromByteBuffer(bb.getValue());
		
		verify(fileChannel);
		assertEquals("foo", s);
	}

	private String getStringFromByteBuffer(ByteBuffer bb) {
		byte[] bytearr = new byte[bb.remaining()];
		bb.get(bytearr);
		String s = new String(bytearr);
		return s;
	}

	@Test
	public void testBufferSizeOutsideTransaction() throws Exception {
		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(bb))).andReturn(3);
		replay(fileChannel);

		writer.write("foo");
		
		verify(fileChannel);
		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testCloseOutsideTransaction() throws Exception {
		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(bb))).andReturn(3);
		expect(fileChannel.write(capture(bb))).andReturn(1);
		replay(fileChannel);

		writer.write("foo");
		writer.close();
		
		verify(fileChannel);
		
		String output = "";
		
		for (ByteBuffer curBuffer : bb.getValues()) {
			output = output + getStringFromByteBuffer(curBuffer);
			
		}
//		assertEquals("foo", getStringFromByteBuffer(writeBuffer.getValue()));
//		assertEquals("c", getStringFromByteBuffer(commitBuffer.getValue()));
		assertEquals("fooc", output);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
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
		writer = new TransactionAwareBufferedWriter(fileChannel, new Runnable() {
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
	@SuppressWarnings({"unchecked", "rawtypes"})
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
	@SuppressWarnings({"unchecked", "rawtypes"})
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
	@SuppressWarnings({"unchecked", "rawtypes"})
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
	
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testExceptionOnFlush() throws Exception {
		final Writer badWriter = new Writer() {
			
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
			}
			
			@Override
			public void flush() throws IOException {
				throw new IOException("This should be bubbled");
			}
			
			@Override
			public void close() throws IOException {
			}
		};
		writer = new TransactionAwareBufferedWriter(fileChannel, new Runnable() {
			public void run() {
				try {
					badWriter.append("c");
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});

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
					return null;
				}
			});
			
			fail("Exception was not thrown");
		} catch (FlushFailedException ffe) {
			assertEquals("Could not write to output buffer", ffe.getMessage());
		}
	}
}
