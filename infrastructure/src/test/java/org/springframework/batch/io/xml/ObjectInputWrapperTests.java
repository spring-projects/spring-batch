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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.xml.xstream.XStreamFactory.ObjectInputWrapper;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Unit tests for {@link ObjectInputWrapper}.
 * @author peter.zozom
 */
public class ObjectInputWrapperTests extends TestCase {

	private ObjectInputWrapper wrapper;

	private MockControl readerControl;

	private XMLStreamReader reader;

	private MockControl oiControl;

	private ObjectInput input;

	public void setUp() throws FileNotFoundException, XMLStreamException {

		// create mock reader
		readerControl = MockControl.createControl(XMLStreamReader.class);
		reader = (XMLStreamReader) readerControl.getMock();

		// create mock for java.io.ObjectInput
		oiControl = MockControl.createControl(ObjectInput.class);
		input = (ObjectInput) oiControl.getMock();

		// create ObjectInputWrapper
		wrapper = new ObjectInputWrapper(reader, input);
	}

	/**
	 * Test {@link ObjectInputWrapper#position()}.
	 */
	public void testPosition() {

		// create mock for Location
		MockControl locationControl = MockControl.createControl(Location.class);
		Location location = (Location) locationControl.getMock();
		location.getLineNumber();
		locationControl.setReturnValue(104);
		locationControl.replay();

		// set up reader mock
		reader.getLocation();
		readerControl.setReturnValue(location);
		readerControl.replay();

		assertEquals(104, wrapper.position());

		readerControl.verify();
		locationControl.verify();
	}

	/**
	 * Test {@link ObjectInputWrapper#readObject()}
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void testReadObject() throws ClassNotFoundException, IOException {

		// set up objectInput mock
		input.readObject();
		oiControl.setReturnValue(this);
		oiControl.replay();

		// read object
		assertSame(this, wrapper.readObject());

		oiControl.verify();
	}

	public void testClose() throws XMLStreamException, IOException {

		// TEST CLOSE

		// set up reader mock
		reader.close();
		readerControl.replay();

		// set up objectInput mock
		input.close();
		oiControl.replay();

		wrapper.close();

		readerControl.verify();
		oiControl.verify();

		// TEST CLOSE WITH XMLStreamException

		// set up reader mock
		readerControl.reset();
		reader.close();
		readerControl.setThrowable(new XMLStreamException());
		readerControl.replay();

		// set up objectInput mock
		oiControl.reset();
		input.close();
		oiControl.replay();

		try {
			wrapper.close();
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException darfe) {
			assertTrue(darfe.getCause() instanceof XMLStreamException);
		}

		readerControl.verify();
		oiControl.verify();

		// TEST CLOSE WITH IOException
		// set up reader mock
		readerControl.reset();
		readerControl.replay();

		// set up objectInput mock
		oiControl.reset();
		input.close();
		oiControl.setThrowable(new IOException());
		oiControl.replay();

		try {
			wrapper.close();
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException darfe) {
			assertTrue(darfe.getCause() instanceof IOException);
		}

		readerControl.verify();
		oiControl.verify();
	}
}
