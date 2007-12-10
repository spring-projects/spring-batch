package org.springframework.batch.io.file.support.stax;

import javax.xml.stream.XMLEventReader;

/**
 * Deserializes XML fragment to domain object. 
 * XML fragment is a standalone XML document corresponding to a single record.
 * 
 * @author Robert Kasanicky
 */
public interface FragmentDeserializer {

	Object deserializeFragment(XMLEventReader eventReader);
}
