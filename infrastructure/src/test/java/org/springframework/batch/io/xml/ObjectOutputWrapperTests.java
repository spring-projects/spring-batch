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

import java.io.IOException;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.xml.xstream.XStreamFactory.ObjectOutputWrapper;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Unit tests for {@link ObjectOutputWrapper}.
 * @author peter.zozom
 */
public class ObjectOutputWrapperTests extends TestCase {

	private ObjectOutputWrapper wrapper;

	private MockControl writerControl;

	private XMLStreamWriter writer;

	private MockControl ooControl;

	private ObjectOutput output;

	private MockFileChannel channel;

	public void setUp() {

		// create mock for xml writer
		writerControl = MockControl.createControl(XMLStreamWriter.class);
		writer = (XMLStreamWriter) writerControl.getMock();

		// create mock for java.io.objectOutput
		ooControl = MockControl.createControl(ObjectOutput.class);
		output = (ObjectOutput) ooControl.getMock();

		// create mock for file channel
		channel = new MockFileChannel();

		// create wrapper
		wrapper = new ObjectOutputWrapper(writer, channel, output);
	}

	public void testAfterRestart() throws XMLStreamException, IOException {

		// set up writer mock
		writer.writeComment("");
		writerControl.replay();

		// set up objectOutput mock
		output.flush();
		ooControl.replay();

		// call after restart
		wrapper.afterRestart(new Long(99));

		// check size and position
		assertEquals(99, channel.size());
		assertEquals(99, channel.position());

		// TEST EXCEPTION HANDLING

		// set up writer mock
		writerControl.reset();
		writer.writeComment("");
		writerControl.setThrowable(new XMLStreamException());
		writerControl.replay();

		try {
			wrapper.afterRestart(new Long(74));
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			assertTrue(bce.getCause() instanceof XMLStreamException);
		}

		// set up writer mock
		writerControl.reset();
		writer.writeComment("");
		writerControl.replay();

		// set up objectOutput mock
		ooControl.reset();
		output.flush();
		ooControl.setThrowable(new IOException());
		ooControl.replay();

		try {
			wrapper.afterRestart(new Long(63));
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			assertTrue(bce.getCause() instanceof IOException);
		}
	}

	public void testClose() throws IOException, XMLStreamException {

		// set up objectOutput mock
		output.close();
		ooControl.replay();

		// set up writer mock
		writer.close();
		writerControl.replay();

		wrapper.close();

		// test whether channel, writer and output were closed
		assertTrue(channel.isClosed());
		ooControl.verify();
		writerControl.verify();

		// TEST EXCEPTION HANDLING

		ooControl.reset();
		output.close();
		ooControl.setThrowable(new IOException());
		ooControl.replay();

		try {
			wrapper.close();
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			assertTrue(bce.getCause() instanceof IOException);
		}

		ooControl.reset();
		output.close();
		ooControl.replay();

		writerControl.reset();
		writer.close();
		writerControl.setThrowable(new XMLStreamException());
		writerControl.replay();

		try {
			wrapper.close();
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			assertTrue(bce.getCause() instanceof XMLStreamException);
		}

	}

	/**
	 * Test flush() method.
	 * @throws IOException
	 */
	public void testFlush() throws IOException {

		// set up objectOutput mock (second call of flush() method will throw an
		// IOException)
		output.flush();
		output.flush();
		ooControl.setThrowable(new IOException());
		ooControl.replay();

		// call flush() twice
		wrapper.flush();
		try {
			wrapper.flush();
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			assertTrue(bce.getCause() instanceof IOException);
		}

		// verify method calls
		ooControl.verify();
	}

	/**
	 * Test position() and position(int) methods.
	 * @throws IOException
	 */
	public void testPosition() throws IOException {

		// set up fileChannel mock
		channel.position(35);

		// test position()
		assertEquals(35, wrapper.position());

		// test position(int)
		wrapper.position(93);
		assertEquals(93, channel.position());

		// set exception
		channel.setThrowable(new IOException());

		// test exception handling
		try {
			wrapper.position();
			fail("BatchEnviromentException was expected");
		}
		catch (DataAccessResourceFailureException bee) {
			assertTrue(bee.getCause() instanceof IOException);
		}

		try {
			wrapper.position(33);
			fail("BatchEnviromentException was expected");
		}
		catch (DataAccessResourceFailureException bee) {
			assertTrue(bee.getCause() instanceof IOException);
		}
	}

	/**
	 * Test size() and truncate() methods.
	 * @throws IOException
	 */
	public void testSizeAndTruncate() throws IOException {

		// set up fileChannel mock
		channel.truncate(53);

		// test size()
		assertEquals(53, wrapper.size());

		// test truncate(int)
		wrapper.truncate(39);
		assertEquals(39, channel.size());

		// set exception
		channel.setThrowable(new IOException());

		// test exception handling
		try {
			wrapper.size();
			fail("BatchEnviromentException was expected");
		}
		catch (DataAccessResourceFailureException bee) {
			assertTrue(bee.getCause() instanceof IOException);
		}

		try {
			wrapper.truncate(66);
			fail("BatchEnviromentException was expected");
		}
		catch (DataAccessResourceFailureException bee) {
			assertTrue(bee.getCause() instanceof IOException);
		}

	}

	/**
	 * Test writeObject() method.
	 * @throws IOException
	 */
	public void testWriteObject() throws IOException {
		//TODO why is "this" used as argument to writeObject?
		
		// set up objectOutput mock
		output.writeObject(this);
		ooControl.replay();

		// write object
		wrapper.writeObject(this);

		// verify method calls
		ooControl.verify();
	}

	/*
	 * Mock for FileChannel
	 */
	private static class MockFileChannel extends FileChannel {

		private long position;

		private long size;

		private boolean closed = false;

		private IOException throwable;

		public void setThrowable(IOException throwable) {
			this.throwable = throwable;
		}

		public long position() throws IOException {
			if (throwable != null) {
				throw throwable;
			}
			return position;
		}

		public FileChannel position(long newPosition) throws IOException {
			if (throwable != null) {
				throw throwable;
			}
			this.position = newPosition;
			return null;
		}

		public long size() throws IOException {
			if (throwable != null) {
				throw throwable;
			}
			return size;
		}

		public FileChannel truncate(long size) throws IOException {
			if (throwable != null) {
				throw throwable;
			}
			this.size = size;
			return null;
		}

		protected void implCloseChannel() throws IOException {
			closed = true;
		}

		public boolean isClosed() {
			return closed;
		}

		public void force(boolean metaData) throws IOException {
		}

		public FileLock lock(long position, long size, boolean shared) throws IOException {
			return null;
		}

		public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
			return null;
		}

		public int read(ByteBuffer dst) throws IOException {
			return 0;
		}

		public int read(ByteBuffer dst, long position) throws IOException {
			return 0;
		}

		public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
			return 0;
		}

		public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
			return 0;
		}

		public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
			return 0;
		}

		public FileLock tryLock(long position, long size, boolean shared) throws IOException {
			return null;
		}

		public int write(ByteBuffer src) throws IOException {
			return 0;
		}

		public int write(ByteBuffer src, long position) throws IOException {
			return 0;
		}

		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
			return 0;
		}
	}
}
