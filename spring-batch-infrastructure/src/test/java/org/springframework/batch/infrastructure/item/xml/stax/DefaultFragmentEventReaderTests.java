/*
 * Copyright 2008-2022 the original author or authors.
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

import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.xml.EventHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.xml.StaxUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultFragmentEventReader}.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
class DefaultFragmentEventReaderTests {

	// object under test
	private FragmentEventReader fragmentReader;

	// wrapped event fragmentReader
	private XMLEventReader eventReader;

	@BeforeEach
	void setUp() throws Exception {
		String xml = "<root> <fragment> <misc1/> </fragment> <misc2/> <fragment> </fragment> </root>";
		Resource input = new ByteArrayResource(xml.getBytes());
		eventReader = StaxUtils.createDefensiveInputFactory().createXMLEventReader(input.getInputStream());
		fragmentReader = new DefaultFragmentEventReader(eventReader);
	}

	/**
	 * Marked element should be wrapped with StartDocument and EndDocument events. Test
	 * uses redundant peek() calls before nextEvent() in important moments to assure
	 * peek() has no side effects on the inner state of reader.
	 */
	@Test
	void testFragmentWrapping() throws XMLStreamException {

		assertTrue(fragmentReader.hasNext());
		moveCursorBeforeFragmentStart();

		fragmentReader.markStartFragment(); // mark the fragment
		assertEquals("fragment", EventHelper.startElementName(eventReader.peek()));

		// StartDocument inserted before StartElement
		assertTrue(fragmentReader.peek().isStartDocument());
		assertTrue(fragmentReader.nextEvent().isStartDocument());
		// StartElement follows in the next step
		assertEquals("fragment", EventHelper.startElementName(fragmentReader.nextEvent()));

		moveCursorToNextElementEvent(); // misc1 start
		fragmentReader.nextEvent(); // skip it
		moveCursorToNextElementEvent(); // misc1 end
		fragmentReader.nextEvent(); // skip it
		moveCursorToNextElementEvent(); // move to end of fragment

		// expected EndElement, peek first which should have no side effect
		assertEquals("fragment", EventHelper.endElementName(fragmentReader.nextEvent()));
		// inserted EndDocument
		assertTrue(fragmentReader.peek().isEndDocument());
		assertTrue(fragmentReader.nextEvent().isEndDocument());

		// now the reader should behave like the document has finished
		assertNull(fragmentReader.peek());
		assertFalse(fragmentReader.hasNext());

		assertThrows(NoSuchElementException.class, fragmentReader::nextEvent);
	}

	/**
	 * When fragment is marked as processed the cursor is moved after the end of the
	 * fragment.
	 */
	@Test
	void testMarkFragmentProcessed() throws XMLStreamException {
		moveCursorBeforeFragmentStart();

		fragmentReader.markStartFragment(); // mark the fragment start

		// read only one event to move inside the fragment
		XMLEvent startFragment = fragmentReader.nextEvent();
		assertTrue(startFragment.isStartDocument());
		fragmentReader.markFragmentProcessed(); // mark fragment as processed

		fragmentReader.nextEvent(); // skip whitespace
		// the next element after fragment end is <misc2/>
		XMLEvent misc2 = fragmentReader.nextEvent();
		assertEquals("misc2", EventHelper.startElementName(misc2));
	}

	/**
	 * Cursor is moved to the end of the fragment as usually even if nothing was read from
	 * the event reader after beginning of fragment was marked.
	 */
	@Test
	void testMarkFragmentProcessedImmediatelyAfterMarkFragmentStart() throws Exception {
		moveCursorBeforeFragmentStart();

		fragmentReader.markStartFragment();
		fragmentReader.markFragmentProcessed();

		fragmentReader.nextEvent(); // skip whitespace
		// the next element after fragment end is <misc2/>
		XMLEvent misc2 = fragmentReader.nextEvent();
		assertEquals("misc2", EventHelper.startElementName(misc2));
	}

	private void moveCursorToNextElementEvent() throws XMLStreamException {
		XMLEvent event = eventReader.peek();
		while (!event.isStartElement() && !event.isEndElement()) {
			eventReader.nextEvent();
			event = eventReader.peek();
		}
	}

	private void moveCursorBeforeFragmentStart() throws XMLStreamException {
		XMLEvent event = eventReader.peek();
		while (!event.isStartElement() || !EventHelper.startElementName(event).equals("fragment")) {
			eventReader.nextEvent();
			event = eventReader.peek();
		}
	}

}
