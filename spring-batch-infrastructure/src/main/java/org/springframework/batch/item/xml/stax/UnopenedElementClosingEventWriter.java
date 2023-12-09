/*
 * Copyright 2014-2023 the original author or authors.
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

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.springframework.util.StringUtils;

/**
 * Delegating XMLEventWriter, which writes EndElement events that match a given collection
 * of QNames directly to the underlying java.io.Writer instead of to the delegate
 * XMLEventWriter.
 *
 * @author Jimmy Praet
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class UnopenedElementClosingEventWriter extends AbstractEventWriterWrapper {

	private final LinkedList<QName> unopenedElements;

	private final Writer ioWriter;

	public UnopenedElementClosingEventWriter(XMLEventWriter wrappedEventWriter, Writer ioWriter,
			List<QName> unopenedElements) {
		super(wrappedEventWriter);
		this.unopenedElements = new LinkedList<>(unopenedElements);
		this.ioWriter = ioWriter;
	}

	@Override
	public void add(XMLEvent event) throws XMLStreamException {
		if (isUnopenedElementCloseEvent(event)) {
			QName element = unopenedElements.removeLast();
			String nsPrefix = !StringUtils.hasText(element.getPrefix()) ? "" : element.getPrefix() + ":";
			try {
				super.flush();
				ioWriter.write("</" + nsPrefix + element.getLocalPart() + ">");
				ioWriter.flush();
			}
			catch (IOException ioe) {
				throw new XMLStreamException("Unable to close tag: " + element, ioe);
			}
		}
		else {
			super.add(event);
		}
	}

	private boolean isUnopenedElementCloseEvent(XMLEvent event) {
		if (unopenedElements.isEmpty()) {
			return false;
		}
		else if (!event.isEndElement()) {
			return false;
		}
		else {
			return unopenedElements.getLast().equals(event.asEndElement().getName());
		}
	}

}
