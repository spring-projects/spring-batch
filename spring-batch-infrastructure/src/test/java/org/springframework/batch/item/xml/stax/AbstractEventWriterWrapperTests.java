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

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.easymock.EasyMock;


/**
 * @author Lucas Ward
 * 
 */
public class AbstractEventWriterWrapperTests extends TestCase {

	AbstractEventWriterWrapper eventWriterWrapper;

	XMLEventWriter xmlEventWriter;

	protected void setUp() throws Exception {
		super.setUp();

		xmlEventWriter = createMock(XMLEventWriter.class);
		eventWriterWrapper = new StubEventWriter(xmlEventWriter);
	}

	public void testAdd() throws XMLStreamException {

		XMLEvent event =  EasyMock.createMock(XMLEvent.class);
		xmlEventWriter.add(event);
		expectLastCall();
		replay(xmlEventWriter);
		eventWriterWrapper.add(event);
		verify(xmlEventWriter);

	}

	public void testAddReader() throws XMLStreamException {

		XMLEventReader reader = createMock(XMLEventReader.class);
		xmlEventWriter.add(reader);
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.add(reader);
		verify(xmlEventWriter);
	}

	public void testClose() throws XMLStreamException {
		xmlEventWriter.close();
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.close();
		verify(xmlEventWriter);
	}

	public void testFlush() throws XMLStreamException {
		xmlEventWriter.flush();
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.flush();
		verify(xmlEventWriter);
	}

	public void testGetNamespaceContext() {
		NamespaceContext context = EasyMock.createMock(NamespaceContext.class);
		expect(xmlEventWriter.getNamespaceContext()).andReturn(context);
		replay(xmlEventWriter);
		assertEquals(eventWriterWrapper.getNamespaceContext(), context);
		verify(xmlEventWriter);
	}

	public void testGetPrefix() throws XMLStreamException {
		String uri = "uri";
		expect(xmlEventWriter.getPrefix(uri)).andReturn(uri);
		replay(xmlEventWriter);
		assertEquals(eventWriterWrapper.getPrefix(uri), uri);
		verify(xmlEventWriter);
	}

	public void testSetDefaultNamespace() throws XMLStreamException {
		String uri = "uri";
		xmlEventWriter.setDefaultNamespace(uri);
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.setDefaultNamespace(uri);
		verify(xmlEventWriter);
	}

	public void testSetNamespaceContext() throws XMLStreamException {

		NamespaceContext context = EasyMock.createMock(NamespaceContext.class);
		xmlEventWriter.setNamespaceContext(context);
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.setNamespaceContext(context);
		verify(xmlEventWriter);
	}

	public void testSetPrefix() throws XMLStreamException {

		String uri = "uri";
		String prefix = "prefix";
		xmlEventWriter.setPrefix(prefix, uri);
		expectLastCall().once();
		replay(xmlEventWriter);
		eventWriterWrapper.setPrefix(prefix, uri);
		verify(xmlEventWriter);
	}

	private static class StubEventWriter extends AbstractEventWriterWrapper {
		public StubEventWriter(XMLEventWriter wrappedEventWriter) {
			super(wrappedEventWriter);
		}
	}
}
