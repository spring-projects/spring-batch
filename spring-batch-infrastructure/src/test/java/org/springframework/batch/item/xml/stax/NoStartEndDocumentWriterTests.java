/*
 * Copyright 2008-2013 the original author or authors.
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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link NoStartEndDocumentStreamWriter}
 * 
 * @author Robert Kasanicky
 * @author Will Schipp
 */
public class NoStartEndDocumentWriterTests extends TestCase {

	// object under test
	private NoStartEndDocumentStreamWriter writer;

	private XMLEventWriter wrappedWriter;

	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    @Override
	protected void setUp() throws Exception {
		wrappedWriter = mock(XMLEventWriter.class);
		writer = new NoStartEndDocumentStreamWriter(wrappedWriter);
	}

	/**
	 * StartDocument and EndDocument events are not passed to the wrapped
	 * writer.
	 */
	public void testNoStartEnd() throws Exception {
		XMLEvent event = eventFactory.createComment("testEvent");

		// mock expects only a single event
		wrappedWriter.add(event);

		writer.add(eventFactory.createStartDocument());
		writer.add(event);
		writer.add(eventFactory.createEndDocument());

	}
	
	/**
	 * Close is not delegated to the wrapped writer. Instead, the wrapped writer is flushed.
	 */
	public void testClose() throws Exception {
		writer.close();
		
		verify(wrappedWriter, times(1)).flush();
		verify(wrappedWriter, never()).close();
	}
}
