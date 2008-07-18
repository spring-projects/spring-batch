package org.springframework.batch.item.xml;

import javax.xml.stream.XMLEventReader;

/**
 * Deserializes XML fragment to domain object. 
 * XML fragment is a standalone XML document corresponding to a single record.
 * 
 * @author Robert Kasanicky
 */
public interface EventReaderDeserializer<T> {

	T deserializeFragment(XMLEventReader eventReader);
}
