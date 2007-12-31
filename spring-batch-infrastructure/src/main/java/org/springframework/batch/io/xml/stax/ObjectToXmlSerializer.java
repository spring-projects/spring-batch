package org.springframework.batch.io.xml.stax;

import javax.xml.stream.XMLEventWriter;

/**
 * Interface wrapping the serialization of an object
 * to xml.  Primarily useful for abstracting how an object
 * is serialized to an XMLEvent from a specific marshaller.
 *
 * @author Lucas Ward
 *
 */
public interface ObjectToXmlSerializer {

	/**
	 * Set event writer objects should be serialized to.
	 *
	 * @param writer
	 */
	void setEventWriter(XMLEventWriter writer);

	/**
	 * Serialize an Object.
	 *
	 * @param output
	 */
	void serializeObject(Object output);
}
