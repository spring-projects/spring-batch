package org.springframework.batch.io.xml.stax;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

/**
 * Tests for {@link EventSequence}
 * 
 * @author Robert Kasanicky
 */
public class EventSequenceTests extends TestCase {

	// object under test
	private EventSequence seq = new EventSequence();
	
	private XMLEventFactory factory = XMLEventFactory.newInstance();

	/**
	 * Common usage scenario.
	 */
	public void testCommonUse() {
		XMLEvent event1 = factory.createComment("testString1");
		XMLEvent event2 = factory.createCData("testString2");
		seq.addEvent(event1);
		seq.addEvent(event2);
		
		assertTrue(seq.hasNext());
		assertSame(event1, seq.nextEvent());
		assertTrue(seq.hasNext());
		assertSame(event2, seq.nextEvent());
		assertFalse(seq.hasNext());
		assertNull(seq.nextEvent());
		
	}
}
