package org.springframework.batch.io.xml;

import javax.xml.stream.XMLEventReader;

/**
 * Deserializes XML fragment to domain object. 
 * XML fragment is a standalone XML document corresponding to a single record.
 * 
 * @author Robert Kasanicky
 */
public interface EventReaderDeserializer {

	Object deserializeFragment(XMLEventReader eventReader);
}
