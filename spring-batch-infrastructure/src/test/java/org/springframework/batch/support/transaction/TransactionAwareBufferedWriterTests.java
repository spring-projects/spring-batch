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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
//import org.easymock.Capture;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Will Schipp
 * 
 */
public class TransactionAwareBufferedWriterTests {

	private FileChannel fileChannel;

	private TransactionAwareBufferedWriter writer;
	
	@Before
	public void init() {
		fileChannel = mock(FileChannel.class);
		
		writer = new TransactionAwareBufferedWriter(fileChannel, new Runnable() {
            @Override
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
		
		writer.setEncoding("UTF-8");
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
//		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
//		when(fileChannel.write(capture(bb))).thenReturn(3);
		when(fileChannel.write(bb.capture())).thenReturn(3);
		fileChannel.force(false);

		writer.write("foo");
		writer.flush();
		// Not closed yet
		
		String s = getStringFromByteBuffer(bb.getValue());
		
		assertEquals("foo", s);
	}

	@Test
	public void testBufferSizeOutsideTransaction() throws Exception {
//		Capture<ByteBuffer> bb = new Capture<ByteBuffer>();
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		writer.write("foo");
		
		assertEquals(0, writer.getBufferSize());
	}
	
	@Ignore //TODO - need to fix capture test
	@Test
	public void testCloseOutsideTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> writeBuffer = ArgumentCaptor.forClass(ByteBuffer.class);
		ArgumentCaptor<ByteBuffer> commitBuffer = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(writeBuffer.capture())).thenReturn(4);
		when(fileChannel.write(commitBuffer.capture())).thenReturn(1);

		writer.write("foo");
		writer.close();
		
		assertEquals("foo", getStringFromByteBuffer(writeBuffer.getValue()));
		assertEquals("c", getStringFromByteBuffer(commitBuffer.getValue()));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testFlushInTransaction() throws Exception {
		when(fileChannel.write((ByteBuffer)anyObject())).thenReturn(3);

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
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
		
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testWriteWithCommit() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);
		
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
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
		
		assertEquals(0, writer.getBufferSize());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testBufferSizeInTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
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
		
		assertEquals(0, writer.getBufferSize());
	}
	
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	// BATCH-1959
	public void testBufferSizeInTransactionWithMultiByteCharacter() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(5);

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write("fóó");
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				assertEquals(5, writer.getBufferSize());
				return null;
			}
		});
		
		assertEquals(0, writer.getBufferSize());
	}	

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testWriteWithRollback() throws Exception {
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                @Override
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
            @Override
			public void run() {
			}
		});

		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                @Override
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
