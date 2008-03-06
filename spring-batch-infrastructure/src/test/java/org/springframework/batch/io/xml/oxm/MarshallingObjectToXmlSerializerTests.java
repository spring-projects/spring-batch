/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.io.xml.oxm;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;

import junit.framework.TestCase;

import org.springframework.batch.io.xml.oxm.MarshallingEventWriterSerializer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

/**
 *
 *
 * @author Lucas Ward
 *
 */
public class MarshallingObjectToXmlSerializerTests extends TestCase {

	MarshallingEventWriterSerializer xmlSerializer;

	MockMarshaller mockMarshaller = new MockMarshaller();

	private StubXmlEventWriter writer;

	protected void setUp() throws Exception {
		super.setUp();

		xmlSerializer = new MarshallingEventWriterSerializer(mockMarshaller);
		writer = new StubXmlEventWriter();
	}

	public void testSuccessfulWrite(){

		Object objectToOutput = new Object();
		xmlSerializer.serializeObject(writer, objectToOutput);
		assertEquals(objectToOutput, mockMarshaller.getMarshalledObject());
	}

	public void testUnsucessfulWrite(){

		mockMarshaller.setThrowException(true);
		try{
			xmlSerializer.serializeObject(writer, new Object());
			fail("Exception expected");
		}catch(DataAccessResourceFailureException ex){
			//expected
		}
	}

	private static class MockMarshaller implements Marshaller{

		private Object marshalledObject;
		private boolean throwException = false;

		public void marshal(Object arg0, Result arg1)
				throws XmlMappingException, IOException {
			if(throwException){
				throw new IOException();
			}
			marshalledObject = arg0;
		}

		public boolean supports(Class arg0) {
			return false;
		}

		public Object getMarshalledObject() {
			return marshalledObject;
		}

		public void setThrowException(boolean throwException) {
			this.throwException = throwException;
		}
	}

	private static class StubXmlEventWriter implements XMLEventWriter{

		public void add(XMLEvent arg0) throws XMLStreamException {		}

		public void add(XMLEventReader arg0) throws XMLStreamException {		}

		public void close() throws XMLStreamException {
		}

		public void flush() throws XMLStreamException {
		}

		public NamespaceContext getNamespaceContext() {
			return null;
		}

		public String getPrefix(String arg0) throws XMLStreamException {
			return null;
		}

		public void setDefaultNamespace(String arg0) throws XMLStreamException {
		}

		public void setNamespaceContext(NamespaceContext arg0)
				throws XMLStreamException {
		}

		public void setPrefix(String arg0, String arg1)
				throws XMLStreamException {
		}

	}
}

