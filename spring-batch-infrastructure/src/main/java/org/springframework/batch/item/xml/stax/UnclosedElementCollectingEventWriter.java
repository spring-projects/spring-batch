/*
 * Copyright 2014 the original author or authors.
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

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegating XMLEventWriter, which collects the QNames of elements that were opened but
 * not closed.
 *
 * @author Jimmy Praet
 * @since 3.0
 */
public class UnclosedElementCollectingEventWriter extends AbstractEventWriterWrapper {

	private LinkedList<QName> unclosedElements = new LinkedList<>();

	public UnclosedElementCollectingEventWriter(XMLEventWriter wrappedEventWriter) {
		super(wrappedEventWriter);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.item.xml.stax.AbstractEventWriterWrapper#add(javax.xml.
	 * stream.events.XMLEvent)
	 */
	@Override
	public void add(XMLEvent event) throws XMLStreamException {
		if (event.isStartElement()) {
			unclosedElements.addLast(event.asStartElement().getName());
		}
		else if (event.isEndElement()) {
			unclosedElements.removeLast();
		}
		super.add(event);
	}

	public List<QName> getUnclosedElements() {
		return unclosedElements;
	}

}
