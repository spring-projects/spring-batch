package org.springframework.batch.io.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Class used to wrap XMLEventReader. Events from wrapped reader are stored in
 * {@link EventSequence} to support transactions.
 * 
 * @author tomas.slanina
 */
class DefaultTransactionalEventReader implements TransactionalEventReader, InitializingBean {

	private EventSequence recorder = new EventSequence();

	private XMLEventReader parent;


	/**
	 * Creates instance of this class and wraps XMLEventReader.
	 * 
	 * @param parent event reader to be wrapped.
	 */
	public DefaultTransactionalEventReader(XMLEventReader parent) {
		setParent(parent);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(parent);
	}

	/**
	 * Callback on transaction rollback.
	 */
	public void onRollback() {
		recorder.reset();
	}

	/**
	 * Callback on transacion commit.
	 * 
	 */
	public void onCommit() {
		recorder.clear();
	}

	/**
	 * @return the parent
	 */
	public XMLEventReader getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(XMLEventReader parent) {
		this.parent = parent;
	}

	/**
	 * @param recorder the recorder to set
	 */
	public void setRecorder(EventSequence recorder) {
		this.recorder = recorder;
	}

	/**
	 * Returns the xml event recorder
	 * @return the xml event recorder
	 */
	public EventSequence getRecorder() {
		return recorder;
	}

	/**
	 * Frees any resources associated with this Reader. This method does not
	 * close the underlying input source.
	 * 
	 * @throws XMLStreamException if there are errors freeing associated
	 * resources
	 */
	public void close() throws XMLStreamException {
		parent.close();

	}

	/**
	 * Reads the content of a text-only element. Precondition: the current event
	 * is START_ELEMENT. Postcondition: The current event is the corresponding
	 * END_ELEMENT.
	 * 
	 * @throws XMLStreamException if the current event is not a START_ELEMENT or
	 * if a non text element is encountered
	 */
	public String getElementText() throws XMLStreamException {
		StringBuffer buf = new StringBuffer();
		XMLEvent e = nextEvent();
		if (!e.isStartElement()) {
			throw new XMLStreamException(
					"Precondition for readText is nextEvent().getEventType() == START_ELEMENT (got " + e.getEventType()
							+ ")");
		}

		while (hasNext()) {
			e = peek();
			if (e.isStartElement()) {
				throw new XMLStreamException("Unexpected Element start");
			}
			if (e.isCharacters()) {
				buf.append(((Characters) e).getData());
			}
			if (e.isEndElement()) {
				return buf.toString();
			}
			nextEvent();
		}

		throw new XMLStreamException("Unexpected end of Document");
	}

	/**
	 * Get the value of a feature/property from the underlying implementation
	 * 
	 * @param name The name of the property
	 * @return The value of the property
	 * @throws IllegalArgumentException if the property is not supported
	 */
	public Object getProperty(String name) throws IllegalArgumentException {
		return parent.getProperty(name);
	}

	/**
	 * Check if there are more events. Returns true if there are more events and
	 * false otherwise.
	 * 
	 * @return true if the event reader has more events, false otherwise
	 */
	public boolean hasNext() {
		return recorder.hasNext() || parent.hasNext();
	}

	/**
	 * Get the next XMLEvent
	 * 
	 * @see XMLEvent
	 * @throws XMLStreamException if there is an error with the underlying XML.
	 * @throws NoSuchElementException iteration has no more elements.
	 */
	public XMLEvent nextEvent() throws XMLStreamException {
		if (!recorder.hasNext()) {
			recorder.addEvent(parent.nextEvent());
		}
		return recorder.nextEvent();
	}

	/**
	 * Skips any insignificant space events until a START_ELEMENT or END_ELEMENT
	 * is reached. If anything other than space characters are encountered, an
	 * exception is thrown. This method should be used when processing
	 * element-only content because the parser is not able to recognize
	 * ignorable whitespace if the DTD is missing or not interpreted.
	 * 
	 * @throws XMLStreamException if anything other than space characters are
	 * encountered
	 */
	public XMLEvent nextTag() throws XMLStreamException {
		while (hasNext()) {
			XMLEvent e = nextEvent();
			if (e.isCharacters() && !((Characters) e).isWhiteSpace()) {
				throw new XMLStreamException("Unexpected text");
			}
			if (e.isStartElement() || e.isEndElement()) {
				return e;
			}
		}
		throw new XMLStreamException("Unexpected end of Document");
	}

	/**
	 * Check the next XMLEvent without reading it from the stream. Returns null
	 * if the stream is at EOF or has no more XMLEvents. A call to peek() will
	 * be equal to the next return of next().
	 * 
	 * @see XMLEvent
	 * @throws XMLStreamException
	 */
	public XMLEvent peek() throws XMLStreamException {
		return (recorder.hasNext()) ? recorder.peek() : parent.peek();
	}

	/**
	 * Returns the next element in the iteration. Calling this method repeatedly
	 * until the {@link #hasNext()} method returns false will return each
	 * element in the underlying collection exactly once.
	 * 
	 * @return the next element in the iteration.
	 * @exception NoSuchElementException iteration has no more elements.
	 */
	public Object next() {
		try {
			return nextEvent();
		}
		catch (XMLStreamException e) {
			return null;
		}
	}

	/**
	 * In this implementation throws UnsupportedOperationException.
	 */
	public void remove() {
		throw new java.lang.UnsupportedOperationException();
	}
}
