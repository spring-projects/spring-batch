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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
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
		Capture<ByteBuffer> writeBuffer = new Capture<ByteBuffer>();
		Capture<ByteBuffer> commitBuffer = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(writeBuffer))).andReturn(3);
		expect(fileChannel.write(capture(commitBuffer))).andReturn(1);
		replay(fileChannel);

		writer.write("foo");
		writer.close();
		
		verify(fileChannel);
		
		assertEquals("foo", getStringFromByteBuffer(writeBuffer.getValue()));
		assertEquals("c", getStringFromByteBuffer(commitBuffer.getValue()));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testFlushInTransaction() throws Exception {
		expect(fileChannel.write((ByteBuffer)anyObject())).andReturn(3);
		replay(fileChannel);

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write("foo");
					writer.flush();
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				assertEquals(3, writer.getBufferSize());
				return null;
			}
		});
		
		verify(fileChannel);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testWriteWithCommit() throws Exception {
		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(bb))).andReturn(3);
		replay(fileChannel);
		
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
		
		verify(fileChannel);
		assertEquals(0, writer.getBufferSize());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testBufferSizeInTransaction() throws Exception {
		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		expect(fileChannel.write(capture(bb))).andReturn(3);
		replay(fileChannel);

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
		
		verify(fileChannel);
		assertEquals(0, writer.getBufferSize());
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
					throw new RuntimeException("Planned failure");
				}
			});
			fail("Exception was not thrown");
		}
		catch (RuntimeException e) {
			// expected
			String message = e.getMessage();
			assertEquals("Wrong message:  " + message, "Planned failure", message);
		}
		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testCleanUpAfterRollback() throws Exception {
		testWriteWithRollback();
		testWriteWithCommit();
	}
	
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testExceptionOnFlush() throws Exception {
		writer = new TransactionAwareBufferedWriter(fileChannel, new Runnable() {
			public void run() {
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
					return null;
				}
			});
			
			fail("Exception was not thrown");
		} catch (FlushFailedException ffe) {
			assertEquals("Could not write to output buffer", ffe.getMessage());
		}
	}

	private String getStringFromByteBuffer(ByteBuffer bb) {
		byte[] bytearr = new byte[bb.remaining()];
		bb.get(bytearr);
		String s = new String(bytearr);
		return s;
	}
}
