/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.xml.stax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.Test;

/**
 * @author Lucas Ward
 * @author Will Schipp
 */
class AbstractEventReaderWrapperTests {

	private final XMLEventReader xmlEventReader = mock();

	private final AbstractEventReaderWrapper eventReaderWrapper = new StubEventReader(xmlEventReader);

	@Test
	void testClose() throws XMLStreamException {
		xmlEventReader.close();
		eventReaderWrapper.close();
	}

	@Test
	void testGetElementText() throws XMLStreamException {

		String text = "text";
		when(xmlEventReader.getElementText()).thenReturn(text);
		assertEquals(eventReaderWrapper.getElementText(), text);
	}

	@Test
	void testGetProperty() throws IllegalArgumentException {

		String text = "text";
		when(xmlEventReader.getProperty("name")).thenReturn(text);
		assertEquals(eventReaderWrapper.getProperty("name"), text);
	}

	@Test
	void testHasNext() {

		when(xmlEventReader.hasNext()).thenReturn(true);
		assertTrue(eventReaderWrapper.hasNext());
	}

	@Test
	void testNext() {

		String text = "text";
		when(xmlEventReader.next()).thenReturn(text);
		assertEquals(eventReaderWrapper.next(), text);
	}

	@Test
	void testNextEvent() throws XMLStreamException {

		XMLEvent event = mock();
		when(xmlEventReader.nextEvent()).thenReturn(event);
		assertEquals(eventReaderWrapper.nextEvent(), event);
	}

	@Test
	void testNextTag() throws XMLStreamException {

		XMLEvent event = mock();
		when(xmlEventReader.nextTag()).thenReturn(event);
		assertEquals(eventReaderWrapper.nextTag(), event);
	}

	@Test
	void testPeek() throws XMLStreamException {

		XMLEvent event = mock();
		when(xmlEventReader.peek()).thenReturn(event);
		assertEquals(eventReaderWrapper.peek(), event);
	}

	@Test
	void testRemove() {

		xmlEventReader.remove();
		eventReaderWrapper.remove();
	}

	private static class StubEventReader extends AbstractEventReaderWrapper {

		public StubEventReader(XMLEventReader wrappedEventReader) {
			super(wrappedEventReader);
		}

	}

}
