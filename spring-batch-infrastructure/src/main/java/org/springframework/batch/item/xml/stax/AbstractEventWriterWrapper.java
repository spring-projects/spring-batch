/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.item.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Delegates all functionality to the wrapped writer allowing subclasses to override only
 * the methods they want to change.
 *
 * @author Robert Kasanicky
 */
abstract class AbstractEventWriterWrapper implements XMLEventWriter {

	protected XMLEventWriter wrappedEventWriter;

	public AbstractEventWriterWrapper(XMLEventWriter wrappedEventWriter) {
		this.wrappedEventWriter = wrappedEventWriter;
	}

	@Override
	public void add(XMLEvent event) throws XMLStreamException {
		wrappedEventWriter.add(event);
	}

	@Override
	public void add(XMLEventReader reader) throws XMLStreamException {
		wrappedEventWriter.add(reader);
	}

	@Override
	public void close() throws XMLStreamException {
		wrappedEventWriter.close();
	}

	@Override
	public void flush() throws XMLStreamException {
		wrappedEventWriter.flush();
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		return wrappedEventWriter.getNamespaceContext();
	}

	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		return wrappedEventWriter.getPrefix(uri);
	}

	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		wrappedEventWriter.setDefaultNamespace(uri);
	}

	@Override
	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		wrappedEventWriter.setNamespaceContext(context);
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		wrappedEventWriter.setPrefix(prefix, uri);
	}

}
