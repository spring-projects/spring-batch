package org.springframework.batch.item.xml;

import javax.xml.stream.XMLEventWriter;

/**
 * Interface wrapping the serialization of an object
 * to xml.  Primarily useful for abstracting how an object
 * is serialized to an XMLEvent from a specific marshaller.
 *
 * @author Lucas Ward
 *
 */
public interface EventWriterSerializer {

	/**
	 * Serialize an Object.
	 *
	 * @param output
	 */
	void serializeObject(XMLEventWriter writer, Object output);
}
