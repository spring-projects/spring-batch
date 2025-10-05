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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.xml.stax.NoStartEndDocumentStreamWriter;

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
class NoStartEndDocumentWriterTests {

	private final XMLEventWriter wrappedWriter = mock();

	private final NoStartEndDocumentStreamWriter writer = new NoStartEndDocumentStreamWriter(wrappedWriter);

	private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

	/**
	 * StartDocument and EndDocument events are not passed to the wrapped writer.
	 */
	@Test
	void testNoStartEnd() throws Exception {
		XMLEvent event = eventFactory.createComment("testEvent");

		// mock expects only a single event
		wrappedWriter.add(event);

		writer.add(eventFactory.createStartDocument());
		writer.add(event);
		writer.add(eventFactory.createEndDocument());

	}

	/**
	 * Close is not delegated to the wrapped writer. Instead, the wrapped writer is
	 * flushed.
	 */
	@Test
	void testClose() throws Exception {
		writer.close();

		verify(wrappedWriter, times(1)).flush();
		verify(wrappedWriter, never()).close();
	}

}
