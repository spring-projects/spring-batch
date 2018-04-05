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

import java.util.HashMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesBag;
import org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.value.SimpleCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;
import org.springframework.util.Assert;

/**
 * Helper to generate a interface with string constants of the different paths in a xml file.
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class TagReader extends BasisReader {

	private HashMap<XmlElementPath, XmlElementPath> tags;

	private ValuesAndAttributesBag s;

	/**
	 * Constructor of an uninitialized StaxXmlPathReader
	 * 
	 */
	public TagReader() {
		super();
		tags = new HashMap<>();
		SimpleCurrentObject current = new SimpleCurrentObject();
		ValueContainer map = new ValueContainer();
		s = new ValuesAndAttributesContainer(current, map);
	}

	/**
	 * read the next Object from the Stax-Stream
	 * 
	 * @throws XMLStreamException if an error occurred
	 */
	public void read() throws XMLStreamException {
		try {
			while (xmlr.hasNext()) {
				next(xmlr);
				xmlr.next();
			}
		}
		catch (XMLStreamException e) {
			Messages.throwReaderRuntimeException(e, "Runtime.FILE_PROCESSING");
		}

	}

	/**
	 * create the source text for a class in a package und classname
	 * 
	 * @param packageName the package name of the generated source
	 * @param className the class name of the generated source
	 * @return the generated source text
	 */
	public String source(String packageName, String className) {
		Assert.hasText(packageName, "The packagename should not be empty");
		Assert.hasText(className, "The classname should not be empty");

		StringBuilder builder = new StringBuilder();
		builder.append("package " + packageName + ";\n");
		builder.append("public interface " + className + " {\n");
		for (XmlElementPath name : tags.keySet()) {
			String[] umgekehrt = name.toString().substring(1).split("\\/");
			builder.append(" String ");
			for (int i = umgekehrt.length - 1; i >= 0; i--) {
				builder.append(umgekehrt[i].replaceAll("\\@", "At"));
				if (i > 0) {
					builder.append("_");
				}
			}
			builder.append(" = \"" + name + "\";\n");
		}
		builder.append("}\n\n");
		return builder.toString();
	}

	@Override
	protected void nextText(XMLStreamReader xmlr) {
		int start = xmlr.getTextStart();
		int length = xmlr.getTextLength();
		s.setText(new String(xmlr.getTextCharacters(), start, length));
	}

	@Override
	protected void nextEndElement(XMLStreamReader xmlr) {
		if (xmlr.hasName()) {
			s.pop();
		}
	}

	@Override
	protected void nextStartElement(XMLStreamReader xmlr) {
		if (xmlr.hasName()) {
			s.push(xmlr.getName());
			XmlElementPath pfad = s.getCurrentPath();
			tags.put(pfad, pfad);
		}
		processAttributes(xmlr);
	}

	protected void processAttribute(XMLStreamReader xmlr, int index) {
		String localName = xmlr.getAttributeLocalName(index);
		s.push(new QName("@" + localName));
		XmlElementPath pfad = s.getCurrentPath();
		tags.put(pfad, pfad);
		s.pop();

	}

}
