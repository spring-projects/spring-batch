/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.batch.item.xmlpathreader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * The BasisReader is a base class for reading XML Files with a StaxReader
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public abstract class BasisReader {

	protected XMLStreamReader xmlr;

	/**
	 * Constructor for super class
	 */
	protected BasisReader() {
		super();
	}


	/**
	 * start to read from a Resource
	 *  
	 * @param input the Resource with the XML content
	 *  
	 */
	public void read(Resource input) {
		Assert.notNull(input, "The resource should not be null");
		try {
			read(input.getInputStream());
		} catch ( IOException e) {
			Messages.throwReaderRuntimeException(e, "Runtime.FILE_PROCESSING");
		}
	}


	
	
	/**
	 * start to read from an InputStream
	 * 
	 * @param inputStream the InputStream with the XML content
	 */
	public void read(InputStream inputStream) {
		Assert.notNull(inputStream, "The inputStream should not be null");

		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try {
			xmlr = xmlif.createXMLStreamReader(inputStream);
		}
		catch ( XMLStreamException e) {
			Messages.throwReaderRuntimeException(e, "Runtime.FILE_PROCESSING");
		}
	}

	
	/**
	 * start to read a file
	 * 
	 * @param filename name of the file
	 */
	public void read(String filename) {
		Assert.hasText(filename, "The filename should not be empty");

		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try {
			xmlr = xmlif.createXMLStreamReader(filename, new FileInputStream(filename));
		}
		catch (FileNotFoundException | XMLStreamException e) {
			Messages.throwReaderRuntimeException(e, "Runtime.FILE_PROCESSING");
		}
	}

	
	protected void next(XMLStreamReader xmlr) {
		switch (xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
			nextStartElement(xmlr);
			break;
		case XMLStreamConstants.END_ELEMENT:
			nextEndElement(xmlr);
			break;
		case XMLStreamConstants.SPACE:
		case XMLStreamConstants.CHARACTERS:
			nextText(xmlr);
			break;
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
		case XMLStreamConstants.CDATA:
		case XMLStreamConstants.COMMENT:
		case XMLStreamConstants.ENTITY_REFERENCE:
		case XMLStreamConstants.START_DOCUMENT:
		default:
			break;
		}

	}
	
	/**
	 * close the stream
	 * 
	 * @throws XMLStreamException if an error occurred
	 */
	public void close() throws XMLStreamException {
		xmlr.close();
	}

	protected void processAttributes(XMLStreamReader xmlr) {
		for (int i = 0; i < xmlr.getAttributeCount(); i++) {
			processAttribute(xmlr, i);
		}
	}

	protected abstract void processAttribute(XMLStreamReader xmlr, int i);

	protected abstract void nextText(XMLStreamReader xmlr);

	protected abstract void nextEndElement(XMLStreamReader xmlr);

	protected abstract void nextStartElement(XMLStreamReader xmlr);

}