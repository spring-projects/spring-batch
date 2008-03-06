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
package org.springframework.batch.io.xml.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.xml.stax.AbstractEventReaderWrapper;

import com.bea.xml.stream.events.StartDocumentEvent;

/**
 * @author Lucas Ward
 *
 */
public class AbstractEventReaderWrapperTests extends TestCase {

	AbstractEventReaderWrapper eventReaderWrapper;
	MockControl mockEventReaderControl = MockControl.createControl(XMLEventReader.class);
	XMLEventReader xmlEventReader;

	protected void setUp() throws Exception {
		super.setUp();

		xmlEventReader = (XMLEventReader)mockEventReaderControl.getMock();
		eventReaderWrapper = new StubEventReader(xmlEventReader);
	}

	public void testClose() throws XMLStreamException {
		xmlEventReader.close();
		mockEventReaderControl.replay();
		eventReaderWrapper.close();
		mockEventReaderControl.verify();
	}

	public void testGetElementText() throws XMLStreamException {

		String text = "text";
		xmlEventReader.getElementText();
		mockEventReaderControl.setReturnValue(text);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.getElementText(), text);
		mockEventReaderControl.verify();
	}

	public void testGetProperty() throws IllegalArgumentException {

		String text = "text";
		xmlEventReader.getProperty("name");
		mockEventReaderControl.setReturnValue(text);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.getProperty("name"), text);
		mockEventReaderControl.verify();
	}

	public void testHasNext() {

		xmlEventReader.hasNext();
		mockEventReaderControl.setReturnValue(true);
		mockEventReaderControl.replay();
		assertTrue(eventReaderWrapper.hasNext());
		mockEventReaderControl.verify();
	}

	public void testNext() {

		String text = "text";
		xmlEventReader.next();
		mockEventReaderControl.setReturnValue(text);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.next(), text);
		mockEventReaderControl.verify();
	}

	public void testNextEvent() throws XMLStreamException {

		XMLEvent event = new StartDocumentEvent();
		xmlEventReader.nextEvent();
		mockEventReaderControl.setReturnValue(event);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.nextEvent(), event);
		mockEventReaderControl.verify();
	}

	public void testNextTag() throws XMLStreamException {

		XMLEvent event = new StartDocumentEvent();
		xmlEventReader.nextTag();
		mockEventReaderControl.setReturnValue(event);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.nextTag(), event);
		mockEventReaderControl.verify();
	}

	public void testPeek() throws XMLStreamException {

		XMLEvent event = new StartDocumentEvent();
		xmlEventReader.peek();
		mockEventReaderControl.setReturnValue(event);
		mockEventReaderControl.replay();
		assertEquals(eventReaderWrapper.peek(), event);
		mockEventReaderControl.verify();
	}

	public void testRemove() {

		xmlEventReader.remove();
		mockEventReaderControl.replay();
		eventReaderWrapper.remove();
		mockEventReaderControl.verify();
	}

	private static class StubEventReader extends AbstractEventReaderWrapper{
		public StubEventReader(XMLEventReader wrappedEventReader) {
			super(wrappedEventReader);
		}
	}
}
