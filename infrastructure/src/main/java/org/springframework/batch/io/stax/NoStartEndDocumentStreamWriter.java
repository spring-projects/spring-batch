package org.springframework.batch.io.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegating XMLEventWriter, which ignores start and end document events,
 * but passes through everything else.
 * 
 * @author peter.zozom
 */
class NoStartEndDocumentStreamWriter implements XMLEventWriter {

	private XMLEventWriter delegate;
	
	public NoStartEndDocumentStreamWriter(XMLEventWriter delegate) {
		this.delegate = delegate;
	}
		
	public void add(XMLEvent event) throws XMLStreamException {
		if ((!event.isStartDocument()) && (!event.isEndDocument())) {
			delegate.add(event);
		}
	}

	public void add(XMLEventReader reader) throws XMLStreamException {
		delegate.add(reader);
	}

	public void close() throws XMLStreamException {
		delegate.close();
	}

	public void flush() throws XMLStreamException {
		delegate.flush();
	}

	public NamespaceContext getNamespaceContext() {
		return delegate.getNamespaceContext();
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return delegate.getPrefix(uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		delegate.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		delegate.setNamespaceContext(context);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		delegate.setPrefix(prefix, uri);
	}

}
