/*
 * Copyright 2008 the original author or authors.
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
