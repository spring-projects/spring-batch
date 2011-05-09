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

import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Default implementation of {@link FragmentEventReader}
 * 
 * @author Robert Kasanicky
 */
public class DefaultFragmentEventReader extends AbstractEventReaderWrapper implements FragmentEventReader {

	// true when the next event is the StartElement of next fragment
	private boolean startFragmentFollows = false;

	// true when the next event is the EndElement of current fragment
	private boolean endFragmentFollows = false;

	// true while cursor is inside fragment
	private boolean insideFragment = false;

	// true when reader should behave like the cursor was at the end of document
	private boolean fakeDocumentEnd = false;

	private StartDocument startDocumentEvent = null;

	private EndDocument endDocumentEvent = null;

	// fragment root name is remembered so that the matching closing element can
	// be identified
	private QName fragmentRootName = null;

	// counts the occurrences of current fragmentRootName (increased for
	// StartElement, decreased for EndElement)
	private int matchCounter = 0;

	/**
	 * Caches the StartDocument event for later use.
	 * @param wrappedEventReader the original wrapped event reader
	 */
	public DefaultFragmentEventReader(XMLEventReader wrappedEventReader) {
		super(wrappedEventReader);
		try {
			startDocumentEvent = (StartDocument) wrappedEventReader.peek();
		}
		catch (XMLStreamException e) {
			throw new DataAccessResourceFailureException("Error reading start document from event reader", e);
		}

		endDocumentEvent = XMLEventFactory.newInstance().createEndDocument();
	}

	public void markStartFragment() {
		startFragmentFollows = true;
		fragmentRootName = null;
	}

	public boolean hasNext() {
		try {
			if (peek() != null) {
				return true;
			}
		}
		catch (XMLStreamException e) {
			throw new DataAccessResourceFailureException("Error reading XML stream", e);
		}
		return false;
	}

	public Object next() {
		try {
			return nextEvent();
		}
		catch (XMLStreamException e) {
			throw new DataAccessResourceFailureException("Error reading XML stream", e);
		}
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		if (fakeDocumentEnd) {
			throw new NoSuchElementException();
		}
		XMLEvent event = wrappedEventReader.peek();
		XMLEvent proxyEvent = alterEvent(event, false);
		checkFragmentEnd(proxyEvent);
		if (event == proxyEvent) {
			wrappedEventReader.nextEvent();
		}

		return proxyEvent;
	}

	/**
	 * Sets the endFragmentFollows flag to true if next event is the last event
	 * of the fragment.
	 * @param event peek() from wrapped event reader
	 */
	private void checkFragmentEnd(XMLEvent event) {
		if (event.isStartElement() && ((StartElement) event).getName().equals(fragmentRootName)) {
			matchCounter++;
		}
		else if (event.isEndElement() && ((EndElement) event).getName().equals(fragmentRootName)) {
			matchCounter--;
			if (matchCounter == 0) {
				endFragmentFollows = true;
			}
		}
	}

	/**
	 * @param event peek() from wrapped event reader
	 * @param peek if true do not change the internal state
	 * @return StartDocument event if peek() points to beginning of fragment
	 * EndDocument event if cursor is right behind the end of fragment original
	 * event otherwise
	 */
	private XMLEvent alterEvent(XMLEvent event, boolean peek) {
		if (startFragmentFollows) {
			fragmentRootName = ((StartElement) event).getName();
			if (!peek) {
				startFragmentFollows = false;
				insideFragment = true;
			}
			return startDocumentEvent;
		}
		else if (endFragmentFollows) {
			if (!peek) {
				endFragmentFollows = false;
				insideFragment = false;
				fakeDocumentEnd = true;
			}
			return endDocumentEvent;
		}
		return event;
	}

	public XMLEvent peek() throws XMLStreamException {
		if (fakeDocumentEnd) {
			return null;
		}
		return alterEvent(wrappedEventReader.peek(), true);
	}

	/**
	 * Finishes reading the fragment in case the fragment was processed without
	 * being read until the end.
	 */
	public void markFragmentProcessed() {
		if (insideFragment|| startFragmentFollows) {
			try {
				while (!(nextEvent() instanceof EndDocument)) {
					// just read all events until EndDocument
				}
			}
			catch (XMLStreamException e) {
				throw new DataAccessResourceFailureException("Error reading XML stream", e);
			}
		}
		fakeDocumentEnd = false;
	}

	public void reset() {
		insideFragment = false;
		startFragmentFollows = false;
		endFragmentFollows = false;
		fakeDocumentEnd = false;
		fragmentRootName = null;
		matchCounter = 0;
	}

}
