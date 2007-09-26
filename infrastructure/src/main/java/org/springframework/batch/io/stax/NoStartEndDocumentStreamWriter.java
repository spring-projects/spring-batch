package org.springframework.batch.io.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegating XMLEventWriter, which ignores start and end document events,
 * but passes through everything else.
 *
 * @author peter.zozom
 * @author Robert Kasanicky
 */
class NoStartEndDocumentStreamWriter extends AbstractEventWriterWrapper {

	public NoStartEndDocumentStreamWriter(XMLEventWriter wrappedEventWriter) {
		super(wrappedEventWriter);
	}

	public void add(XMLEvent event) throws XMLStreamException {
		if ((!event.isStartDocument()) && (!event.isEndDocument())) {
			wrappedEventWriter.add(event);
		}
	}
}
