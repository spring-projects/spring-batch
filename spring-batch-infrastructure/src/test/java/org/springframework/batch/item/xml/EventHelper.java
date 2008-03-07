package org.springframework.batch.item.xml;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Helper methods for working with XML Events.
 * 
 * @author Robert Kasanicky
 */
public class EventHelper {

	//utility class
	private EventHelper() {}
	
	/**
	 * @return element name assuming the event is instance of StartElement
	 */
	public static String startElementName(XMLEvent event) {
		return ((StartElement) event).getName().getLocalPart();
	}
	
	/**
	 * @return element name assuming the event is instance of EndElement
	 */
	public static String endElementName(XMLEvent event) {
		return ((EndElement) event).getName().getLocalPart();
	}
}
