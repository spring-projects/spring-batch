/*
 * Copyright 2006-2012 the original author or authors.
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
package org.springframework.batch.support.transaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

		writer = new TransactionAwareBufferedWriter(fileChannel, () -> {
			try {
				ByteBuffer bb = ByteBuffer.wrap("c".getBytes());
				fileChannel.write(bb);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});

		writer.setEncoding("UTF-8");
	}

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	/**
	 * Test method for
	 * {@link org.springframework.batch.support.transaction.TransactionAwareBufferedWriter#write(java.lang.String)}
	 * .
	 */
	@Test
	public void testWriteOutsideTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		writer.write("foo");
		writer.flush();
		// Not closed yet

		String s = getStringFromByteBuffer(bb.getValue());

		assertEquals("foo", s);

		verify(fileChannel, never()).force(false);
	}

	@Test
	public void testWriteOutsideTransactionForceSync() throws Exception {
		writer.setForceSync(true);
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		writer.write("foo");
		writer.flush();
		// Not closed yet

		String s = getStringFromByteBuffer(bb.getValue());

		assertEquals("foo", s);

		verify(fileChannel, times(1)).force(false);
	}

	@Test
	public void testBufferSizeOutsideTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		writer.write("foo");

		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testCloseOutsideTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> byteBufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

		when(fileChannel.write(byteBufferCaptor.capture())).thenAnswer(invocation -> ((ByteBuffer) invocation.getArguments()[0]).remaining());

		writer.write("foo");
		writer.close();

		assertEquals("foo", getStringFromByteBuffer(byteBufferCaptor.getAllValues().get(0)));
		assertEquals("c", getStringFromByteBuffer(byteBufferCaptor.getAllValues().get(1)));
	}

	@Test
	public void testFlushInTransaction() throws Exception {
		when(fileChannel.write(any(ByteBuffer.class))).thenReturn(3);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("foo");
				writer.flush();
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(3, writer.getBufferSize());
			return null;
		});

		verify(fileChannel, never()).force(false);
	}

	@Test
	public void testFlushInTransactionForceSync() throws Exception {
		writer.setForceSync(true);
		when(fileChannel.write(any(ByteBuffer.class))).thenReturn(3);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("foo");
				writer.flush();
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(3, writer.getBufferSize());
			return null;
		});

		verify(fileChannel, times(1)).force(false);
	}

	@Test
	public void testWriteWithCommit() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("foo");
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(3, writer.getBufferSize());
			return null;
		});

		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testBufferSizeInTransaction() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(3);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("foo");
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(3, writer.getBufferSize());
			return null;
		});

		assertEquals(0, writer.getBufferSize());
	}

	@Test
	// BATCH-1959
	public void testBufferSizeInTransactionWithMultiByteCharacterUTF8() throws Exception {
		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(5);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("fóó");
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(5, writer.getBufferSize());
			return null;
		});

		assertEquals(0, writer.getBufferSize());
	}

	@Test
	// BATCH-1959
	public void testBufferSizeInTransactionWithMultiByteCharacterUTF16BE() throws Exception {
		writer.setEncoding("UTF-16BE");

		ArgumentCaptor<ByteBuffer> bb = ArgumentCaptor.forClass(ByteBuffer.class);
		when(fileChannel.write(bb.capture())).thenReturn(6);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write("fóó");
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			assertEquals(6, writer.getBufferSize());
			return null;
		});

		assertEquals(0, writer.getBufferSize());
	}

	@Test
	public void testWriteWithRollback() throws Exception {
		try {
			new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
				try {
					writer.write("foo");
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				throw new RuntimeException("Planned failure");
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
	public void testExceptionOnFlush() throws Exception {
		writer = new TransactionAwareBufferedWriter(fileChannel, () -> {
		});

		try {
			new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
				try {
					writer.write("foo");
				}
				catch (IOException e) {
					throw new IllegalStateException("Unexpected IOException", e);
				}
				return null;
			});

			fail("Exception was not thrown");
		} catch (FlushFailedException ffe) {
			assertEquals("Could not write to output buffer", ffe.getMessage());
		}
	}
	
	// BATCH-2018
	@Test
	public void testResourceKeyCollision() throws Exception {
		final int limit = 5000;
		final TransactionAwareBufferedWriter[] writers = new TransactionAwareBufferedWriter[limit];
		final String[] results = new String[limit];
		for(int i = 0; i< limit; i++) {
			final int index = i;
			@SuppressWarnings("resource")
			FileChannel fileChannel = mock(FileChannel.class);
			when(fileChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
				ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
				String val = new String(buffer.array(), "UTF-8");
				if(results[index] == null) {
					results[index] = val;
				} else {
					results[index] += val;
				}
				return buffer.limit();
			});
			writers[i] = new TransactionAwareBufferedWriter(fileChannel, null);
		}
		
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				for(int i=0; i< limit; i++) {
					writers[i].write(String.valueOf(i));
				}
			}
			catch (IOException e) {
				throw new IllegalStateException("Unexpected IOException", e);
			}
			return null;
		});
		
		for(int i=0; i< limit; i++) {
			assertEquals(String.valueOf(i), results[i]);
		}				
	}

	private String getStringFromByteBuffer(ByteBuffer bb) {
		byte[] bytearr = new byte[bb.remaining()];
		bb.get(bytearr);
		return new String(bytearr);
	}
}
