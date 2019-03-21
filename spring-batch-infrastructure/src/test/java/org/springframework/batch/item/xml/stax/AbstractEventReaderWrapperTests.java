/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.item.xml.stax;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 * @author Will Schipp
 */
public class AbstractEventReaderWrapperTests extends TestCase {

	AbstractEventReaderWrapper eventReaderWrapper;
	XMLEventReader xmlEventReader;

    @Override
	protected void setUp() throws Exception {
		super.setUp();

		xmlEventReader = mock(XMLEventReader.class);
		eventReaderWrapper = new StubEventReader(xmlEventReader);
	}

	public void testClose() throws XMLStreamException {
		xmlEventReader.close();
		eventReaderWrapper.close();
	}

	public void testGetElementText() throws XMLStreamException {

		String text = "text";
		when(xmlEventReader.getElementText()).thenReturn(text);
		assertEquals(eventReaderWrapper.getElementText(), text);
	}

	public void testGetProperty() throws IllegalArgumentException {

		String text = "text";
		when(xmlEventReader.getProperty("name")).thenReturn(text);
		assertEquals(eventReaderWrapper.getProperty("name"), text);
	}

	public void testHasNext() {

		when(xmlEventReader.hasNext()).thenReturn(true);
		assertTrue(eventReaderWrapper.hasNext());
	}

	public void testNext() {

		String text = "text";
		when(xmlEventReader.next()).thenReturn(text);
		assertEquals(eventReaderWrapper.next(), text);
	}

	public void testNextEvent() throws XMLStreamException {

		XMLEvent event = mock(XMLEvent.class);
		when(xmlEventReader.nextEvent()).thenReturn(event);
		assertEquals(eventReaderWrapper.nextEvent(), event);
	}

	public void testNextTag() throws XMLStreamException {

		XMLEvent event = mock(XMLEvent.class);
		when(xmlEventReader.nextTag()).thenReturn(event);
		assertEquals(eventReaderWrapper.nextTag(), event);
	}

	public void testPeek() throws XMLStreamException {

		XMLEvent event = mock(XMLEvent.class);
		when(xmlEventReader.peek()).thenReturn(event);
		assertEquals(eventReaderWrapper.peek(), event);
	}

	public void testRemove() {

		xmlEventReader.remove();
		eventReaderWrapper.remove();
	}

	private static class StubEventReader extends AbstractEventReaderWrapper {
		public StubEventReader(XMLEventReader wrappedEventReader) {
			super(wrappedEventReader);
		}
	}
}
