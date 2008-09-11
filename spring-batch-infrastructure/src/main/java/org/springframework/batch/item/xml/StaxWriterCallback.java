package org.springframework.batch.item.xml;

import java.io.IOException;

import javax.xml.stream.XMLEventWriter;

/**
 * Callback interface for writing to an XML file - useful e.g. for handling headers
 * and footers.
 * 
 * @author Robert Kasanicky
 */
public interface StaxWriterCallback {

	/**
	 * Write contents using the supplied {@link XMLEventWriter}. It is not
	 * required to flush the writer inside this method.
	 */
	void write(XMLEventWriter writer) throws IOException;
}
