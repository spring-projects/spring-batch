package org.springframework.batch.io.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegates all functionality to the wrapped writer allowing
 * subclasses to override only the methods they want to change.
 * 
 * @author Robert Kasanicky
 */
abstract class AbstractEventWriterWrapper implements XMLEventWriter {
	
	protected XMLEventWriter wrappedEventWriter;

	public AbstractEventWriterWrapper(XMLEventWriter wrappedEventWriter) {
		this.wrappedEventWriter = wrappedEventWriter;
	}

	public void add(XMLEvent event) throws XMLStreamException {
		wrappedEventWriter.add(event);
	}

	public void add(XMLEventReader reader) throws XMLStreamException {
		wrappedEventWriter.add(reader);
	}

	public void close() throws XMLStreamException {
		wrappedEventWriter.close();
	}

	public void flush() throws XMLStreamException {
		wrappedEventWriter.flush();
	}

	public NamespaceContext getNamespaceContext() {
		return wrappedEventWriter.getNamespaceContext();
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return wrappedEventWriter.getPrefix(uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		wrappedEventWriter.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		wrappedEventWriter.setNamespaceContext(context);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		wrappedEventWriter.setPrefix(prefix, uri);
	}
}
