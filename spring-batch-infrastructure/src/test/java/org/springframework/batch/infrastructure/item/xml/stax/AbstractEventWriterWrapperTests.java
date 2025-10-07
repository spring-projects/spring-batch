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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.xml.stax.AbstractEventWriterWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lucas Ward
 * @author Will Schipp
 *
 */
class AbstractEventWriterWrapperTests {

	private final XMLEventWriter xmlEventWriter = mock();

	private final AbstractEventWriterWrapper eventWriterWrapper = new StubEventWriter(xmlEventWriter);

	@Test
	void testAdd() throws XMLStreamException {

		XMLEvent event = mock();
		xmlEventWriter.add(event);
		eventWriterWrapper.add(event);

	}

	@Test
	void testAddReader() throws XMLStreamException {

		XMLEventReader reader = mock();
		xmlEventWriter.add(reader);
		eventWriterWrapper.add(reader);
	}

	@Test
	void testClose() throws XMLStreamException {
		xmlEventWriter.close();
		eventWriterWrapper.close();
	}

	@Test
	void testFlush() throws XMLStreamException {
		xmlEventWriter.flush();
		eventWriterWrapper.flush();
	}

	@Test
	void testGetNamespaceContext() {
		NamespaceContext context = mock();
		when(xmlEventWriter.getNamespaceContext()).thenReturn(context);
		assertEquals(eventWriterWrapper.getNamespaceContext(), context);
	}

	@Test
	void testGetPrefix() throws XMLStreamException {
		String uri = "uri";
		when(xmlEventWriter.getPrefix(uri)).thenReturn(uri);
		assertEquals(eventWriterWrapper.getPrefix(uri), uri);
	}

	@Test
	void testSetDefaultNamespace() throws XMLStreamException {
		String uri = "uri";
		xmlEventWriter.setDefaultNamespace(uri);
		eventWriterWrapper.setDefaultNamespace(uri);
	}

	@Test
	void testSetNamespaceContext() throws XMLStreamException {

		NamespaceContext context = mock();
		xmlEventWriter.setNamespaceContext(context);
		eventWriterWrapper.setNamespaceContext(context);
	}

	@Test
	void testSetPrefix() throws XMLStreamException {

		String uri = "uri";
		String prefix = "prefix";
		xmlEventWriter.setPrefix(prefix, uri);
		eventWriterWrapper.setPrefix(prefix, uri);
	}

	private static class StubEventWriter extends AbstractEventWriterWrapper {

		public StubEventWriter(XMLEventWriter wrappedEventWriter) {
			super(wrappedEventWriter);
		}

	}

}
