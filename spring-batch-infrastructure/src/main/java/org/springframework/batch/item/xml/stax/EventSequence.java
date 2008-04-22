package org.springframework.batch.item.xml.stax;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.XMLEvent;

/**
 * Holds a list of XML events, typically corresponding to a single record.
 *  
 * @author tomas.slanina
 */
class EventSequence {
	
	private static final int BEFORE_BEGINNING = -1;
	
	private List events;
	
	private int currentIndex;
	
	/**
	 * Creates instance of this class.
	 *
	 */
	public EventSequence() {
		init();
	}
	
	/**
	 * Adds event to the list of stored events.
	 * 
	 * @param event
	 */
	public void addEvent(XMLEvent event) {
		events.add(event);
	}
	
	/**
	 * Gets next XMLEvent from cache and moves cursor to next event.
	 * If cache contains no more events, null is returned.
	 */
	public XMLEvent nextEvent() {
		return (hasNext()) ? (XMLEvent)events.get(++currentIndex) :null;
 	}
	
	/**
	 * Gets next XMLEvent from cache but cursor remains on the same position.
	 * If cache contains no more events, null is returned.
	 */
	public XMLEvent peek() {
		return (hasNext()) ? (XMLEvent)events.get(currentIndex+1) :null;
	}
	
	/**
	 * Removes events from the internal cache.
	 *
	 */
	public void clear() {
		init();
	}
	
	/**
	 * Resets cursor to the cache start.
	 *
	 */
	public void reset() {
		currentIndex = BEFORE_BEGINNING;
	}
	
	/**
	 * Check if there are more events. Returns true if there are more events and
	 * false otherwise.
	 * 
	 * @return true if the event reader has more events, false otherwise
	 */
	public boolean hasNext() {
		return currentIndex + 1 < events.size();
	}
	
	private void init() {
		events = (events != null) ? new ArrayList(events.size())
			: new ArrayList(1000);
		
		reset();
	}
	
	
}
