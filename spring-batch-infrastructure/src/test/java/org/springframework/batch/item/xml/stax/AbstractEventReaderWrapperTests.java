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
package org.springframework.batch.item.xml.stax;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 * 
 */
public class AbstractEventReaderWrapperTests extends TestCase {

	AbstractEventReaderWrapper eventReaderWrapper;
	XMLEventReader xmlEventReader;

	protected void setUp() throws Exception {
		super.setUp();

		xmlEventReader = createMock(XMLEventReader.class);
		eventReaderWrapper = new StubEventReader(xmlEventReader);
	}

	public void testClose() throws XMLStreamException {
		xmlEventReader.close();
		expectLastCall().once();
		replay(xmlEventReader);
		eventReaderWrapper.close();
		verify(xmlEventReader);
	}

	public void testGetElementText() throws XMLStreamException {

		String text = "text";
		expect(xmlEventReader.getElementText()).andReturn(text);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.getElementText(), text);
		verify(xmlEventReader);
	}

	public void testGetProperty() throws IllegalArgumentException {

		String text = "text";
		expect(xmlEventReader.getProperty("name")).andReturn(text);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.getProperty("name"), text);
		verify(xmlEventReader);
	}

	public void testHasNext() {

		expect(xmlEventReader.hasNext()).andReturn(true);
		replay(xmlEventReader);
		assertTrue(eventReaderWrapper.hasNext());
		verify(xmlEventReader);
	}

	public void testNext() {

		String text = "text";
		expect(xmlEventReader.next()).andReturn(text);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.next(), text);
		verify(xmlEventReader);
	}

	public void testNextEvent() throws XMLStreamException {

		XMLEvent event = createMock(XMLEvent.class);
		expect(xmlEventReader.nextEvent()).andReturn(event);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.nextEvent(), event);
		verify(xmlEventReader);
	}

	public void testNextTag() throws XMLStreamException {

		XMLEvent event = createMock(XMLEvent.class);
		expect(xmlEventReader.nextTag()).andReturn(event);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.nextTag(), event);
		verify(xmlEventReader);
	}

	public void testPeek() throws XMLStreamException {

		XMLEvent event = createMock(XMLEvent.class);
		expect(xmlEventReader.peek()).andReturn(event);
		replay(xmlEventReader);
		assertEquals(eventReaderWrapper.peek(), event);
		verify(xmlEventReader);
	}

	public void testRemove() {

		xmlEventReader.remove();
		expectLastCall().once();
		replay(xmlEventReader);
		eventReaderWrapper.remove();
		verify(xmlEventReader);
	}

	private static class StubEventReader extends AbstractEventReaderWrapper {
		public StubEventReader(XMLEventReader wrappedEventReader) {
			super(wrappedEventReader);
		}
	}
}
