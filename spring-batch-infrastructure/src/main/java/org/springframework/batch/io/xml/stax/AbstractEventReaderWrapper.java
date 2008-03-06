package org.springframework.batch.io.xml.stax;

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
