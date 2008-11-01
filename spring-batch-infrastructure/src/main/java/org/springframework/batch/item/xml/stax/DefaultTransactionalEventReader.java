package org.springframework.batch.item.xml.stax;

import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Class used to wrap XMLEventReader. Events from wrapped reader are stored in
 * {@link EventSequence} to support transactions.
 * 
 * @deprecated no longer used, to be removed in 2.0
 *
 * @author Tomas Slanina
 * @author Robert Kasanicky
 */
public class DefaultTransactionalEventReader extends AbstractEventReaderWrapper implements TransactionalEventReader, InitializingBean {

	private EventSequence recorder = new EventSequence();


	/**
	 * Creates instance of this class and wraps XMLEventReader.
	 *
	 * @param wrappedReader event reader to be wrapped.
	 */
	public DefaultTransactionalEventReader(XMLEventReader wrappedReader) {
		super(wrappedReader);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(wrappedEventReader);
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
	 * Check if there are more events. Returns true if there are more events and
	 * false otherwise.
	 *
	 * @return true if the event reader has more events, false otherwise
	 */
	public boolean hasNext() {
		return recorder.hasNext() || wrappedEventReader.hasNext();
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
			recorder.addEvent(wrappedEventReader.nextEvent());
		}
		return recorder.nextEvent();
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
		return (recorder.hasNext()) ? recorder.peek() : wrappedEventReader.peek();
	}


	/**
	 * In this implementation throws UnsupportedOperationException.
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
