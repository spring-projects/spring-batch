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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegates all functionality to the wrapped reader allowing
 * subclasses to override only the methods they want to change.
 * 
 * @author Robert Kasanicky
 */
abstract class AbstractEventReaderWrapper implements XMLEventReader {

	protected XMLEventReader wrappedEventReader;
	
	public AbstractEventReaderWrapper(XMLEventReader wrappedEventReader) {
		this.wrappedEventReader = wrappedEventReader;
	}
	
	public void close() throws XMLStreamException {
		wrappedEventReader.close();
		
	}

	public String getElementText() throws XMLStreamException {
		return wrappedEventReader.getElementText();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return wrappedEventReader.getProperty(name);
	}

	public boolean hasNext() {
		return wrappedEventReader.hasNext();
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		return wrappedEventReader.nextEvent();
	}

	public XMLEvent nextTag() throws XMLStreamException {
		return wrappedEventReader.nextTag();
	}

	public XMLEvent peek() throws XMLStreamException {
		return wrappedEventReader.peek();
	}

	public Object next() {
		return wrappedEventReader.next();
	}

	public void remove() {
		wrappedEventReader.remove();
	}

}
