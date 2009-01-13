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

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegating XMLEventWriter, which ignores start and end document events,
 * but passes through everything else.
 *
 * @author peter.zozom
 * @author Robert Kasanicky
 */
public class NoStartEndDocumentStreamWriter extends AbstractEventWriterWrapper {

	public NoStartEndDocumentStreamWriter(XMLEventWriter wrappedEventWriter) {
		super(wrappedEventWriter);
	}

	public void add(XMLEvent event) throws XMLStreamException {
		if ((!event.isStartDocument()) && (!event.isEndDocument())) {
			wrappedEventWriter.add(event);
		}
	}
}
