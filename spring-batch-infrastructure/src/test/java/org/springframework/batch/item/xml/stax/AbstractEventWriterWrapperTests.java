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

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * 
 */
public class AbstractEventWriterWrapperTests extends TestCase {

	AbstractEventWriterWrapper eventWriterWrapper;

	XMLEventWriter xmlEventWriter;

    @Override
	protected void setUp() throws Exception {
		super.setUp();

		xmlEventWriter = mock(XMLEventWriter.class);
		eventWriterWrapper = new StubEventWriter(xmlEventWriter);
	}

	public void testAdd() throws XMLStreamException {

		XMLEvent event =  mock(XMLEvent.class);
		xmlEventWriter.add(event);
		eventWriterWrapper.add(event);

	}

	public void testAddReader() throws XMLStreamException {

		XMLEventReader reader = mock(XMLEventReader.class);
		xmlEventWriter.add(reader);
		eventWriterWrapper.add(reader);
	}

	public void testClose() throws XMLStreamException {
		xmlEventWriter.close();
		eventWriterWrapper.close();
	}

	public void testFlush() throws XMLStreamException {
		xmlEventWriter.flush();
		eventWriterWrapper.flush();
	}

	public void testGetNamespaceContext() {
		NamespaceContext context = mock(NamespaceContext.class);
		when(xmlEventWriter.getNamespaceContext()).thenReturn(context);
		assertEquals(eventWriterWrapper.getNamespaceContext(), context);
	}

	public void testGetPrefix() throws XMLStreamException {
		String uri = "uri";
		when(xmlEventWriter.getPrefix(uri)).thenReturn(uri);
		assertEquals(eventWriterWrapper.getPrefix(uri), uri);
	}

	public void testSetDefaultNamespace() throws XMLStreamException {
		String uri = "uri";
		xmlEventWriter.setDefaultNamespace(uri);
		eventWriterWrapper.setDefaultNamespace(uri);
	}

	public void testSetNamespaceContext() throws XMLStreamException {

		NamespaceContext context = mock(NamespaceContext.class);
		xmlEventWriter.setNamespaceContext(context);
		eventWriterWrapper.setNamespaceContext(context);
	}

	public void testSetPrefix() throws XMLStreamException {

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
