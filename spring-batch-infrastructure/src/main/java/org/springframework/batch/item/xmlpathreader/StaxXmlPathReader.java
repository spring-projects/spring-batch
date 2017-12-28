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

import javax.xml.stream.XMLStreamReader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.attribute.Attribute;
import org.springframework.batch.item.xmlpathreader.attribute.Setter;
import org.springframework.batch.item.xmlpathreader.core.AnnotationProcessor;
import org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.value.Creator;
import org.springframework.batch.item.xmlpathreader.value.CreatorValue;
import org.springframework.batch.item.xmlpathreader.value.CurrentObject;
import org.springframework.batch.item.xmlpathreader.value.SimpleCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;
import org.springframework.util.Assert;

/**
 * The StaxXmlPathReader emit the Objects of the configured Classes. If a end -tag is reached 
 * the next method, emits a instance of the class of this element
 * 
 * The reader must be configured before it is used.
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class StaxXmlPathReader extends BasisReader implements ItemReader<Object> {
	private static final String THE_FIELD_SHOULD_NOT_BE_EMPTY = "The field should not be empty";

	private static final String THE_CLASS_SHOULD_NOT_BE_NULL = "The class should not be null";

	private static final String THE_PATH_SHOULD_NOT_BE_NULL = "The path should not be null";

	private static final String THE_VALUE_SHOULD_NOT_BE_NULL = "The value should not be null";

	private static final String THE_SETTER_SHOULD_NOT_BE_NULL = "The setter should not be null";

	private static final String THE_CREATOR_SHOULD_NOT_BE_NULL = "The creator should not be null";

	private ValuesAndAttributesContainer valuesAndAttributesContainer;


	/**
	 * Constructor of an uninitialized StaxXmlPathReader
	 * 
	 */
	public StaxXmlPathReader() {
		super();
		valuesAndAttributesContainer = new ValuesAndAttributesContainer(new SimpleCurrentObject(),new ValueContainer());
	}

	/**
	 * Constructor with the initialization in annotated classes
	 * 
	 * @param classes the classes with {@link XmlPath} annotations
	 */
	public StaxXmlPathReader(Class<?>... classes) {
		this();
		Assert.noNullElements(classes, "The classes in the array should not be null");
		readAnnotations(classes);
	}

	/**
	 * read the next Object from the Stax-Stream
	 * 
	 */
	@Override
	public Object read() throws Exception {
		CurrentObject current = valuesAndAttributesContainer.getCurrent();
		while (current.isEmpty() && xmlr.hasNext()) {
			next(xmlr);
			xmlr.next();
		}
		if (current.isEmpty()) {
			return null;
		}
		return current.popCurrentObject();
	}

	/**
	 * Add the creation of a class instance to a path of XML Elements
	 * 
	 * @param valuePath path of XML Element, at which the {@link Value} object will be created 
	 * @param clazz class of the generated instance
	 * @return a {@link Value}
	 */
	public Value addValue(XmlElementPath valuePath, Class<?> clazz) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);

		return valuesAndAttributesContainer.addValue(valuePath, clazz);
	}

	/**
	 * Create a {@link CreatorValue}
	 * 
	 * @param valuePath path of XML Element, at which the {@link Value} object will be created
	 * @param clazz class of the generated instance
	 * @param creator the Creator that is creating the object
	 * @return a {@link Value}
	 */
	public Value addValue(XmlElementPath valuePath, Class<?> clazz, Creator creator) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.notNull(creator, THE_CREATOR_SHOULD_NOT_BE_NULL);

		return valuesAndAttributesContainer.addValue(valuePath, clazz, creator);
	}


	/**
	 * Create a {@link CreatorValue}
	 * 
	 * @param valuePath path of XML Element, at which the {@link Value} object will be created
	 * @param clazz class of the generated instance
	 * @param creator the Creator that is creating the object
	 * @return a {@link Value}
	 */
	public Value addValue(String valuePath, Class<?> clazz, Creator creator) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.notNull(creator, THE_CREATOR_SHOULD_NOT_BE_NULL);

		return addValue(new XmlElementPath(valuePath), clazz, creator);
	}

	
	/**
	 * Delagate to an AttributeWithValueContainer
	 * 
	 * @param valuePath the path of XML elements to the object instance, that will be set
	 * @param attributePath the path of XML elements witch text-value, set the {@link Value} attribute 
	 * @param field the name of the setter Method
	 * @return an {@link Attribute}
	 */
	public Attribute addAttribute(XmlElementPath valuePath, XmlElementPath attributePath, String field) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.hasText(field, THE_FIELD_SHOULD_NOT_BE_EMPTY);

		return valuesAndAttributesContainer.addAttribute(valuePath, attributePath, field);
	}

	/**------
	 * create a Attribute from a Setter
	 * 
	 * @param <T> the type of the value
	 * @param <V> the type of the attribute
	 * @param valuePath the path of XML elements to the object instance, that will be set
	 * @param attributePath the path of XML elements witch text-value, set the {@link Value} object
	 * @param handle the Setter that is setting the attribute
	 * @return an {@link Attribute}
	 */
	public <T, V> Attribute addAttribute(XmlElementPath valuePath, XmlElementPath attributePath, Setter<T, V> handle) {
		Assert.notNull(valuePath, THE_VALUE_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(handle, THE_SETTER_SHOULD_NOT_BE_NULL);
		return valuesAndAttributesContainer.addAttribute(valuePath, attributePath, handle);
	}

	/**
	 * create SetAction from a Setter
	 * 
	 * @param <T> the type of the value
	 * @param <V> the type of the attribute
	 * @param valuePath      the path of XML elements to the object instance, that will be set
	 * @param attributePath  the path of XML elements witch text-value, set the {@link Value} object
	 * @param handle         the Setter that is setting the attribute
	 * @return an {@link Attribute}
	 */
	public <T, V> Attribute addAttribute(String valuePath, String attributePath, Setter<T, V> handle) {
		Assert.notNull(valuePath, THE_VALUE_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(handle, THE_SETTER_SHOULD_NOT_BE_NULL);
		return addAttribute(new XmlElementPath(valuePath), new XmlElementPath(attributePath), handle);
	}

	
	/**
	 * Created Object for a Class, perhaps it is not fully instantiated
	 *
	 * @param valuePath  the path of XML elements to the object instance, that will be get
	 * @return the current object of the {@link Value}
	 */
	public Object getValueObject(XmlElementPath valuePath) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);

		return valuesAndAttributesContainer.getValueObject(valuePath);
	}

	/**
	 * Created Object for a Class, perhaps it is not fully instantiated with a Exception if the value or Object does not
	 * exist
	 * 
	 * @param valuePath  the path of XML elements to the object instance, that will be get
	 * @return the current object of the {@link Value}
	 */
	public Object getValueObjectWithException(XmlElementPath valuePath) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);

		return valuesAndAttributesContainer.getValueObjectWithException(valuePath);
	}

	@Override
	protected void nextText(XMLStreamReader xmlr) {
		int start = xmlr.getTextStart();
		int length = xmlr.getTextLength();
		valuesAndAttributesContainer.setText(new String(xmlr.getTextCharacters(), start, length));
	}

	@Override
	protected void nextEndElement(XMLStreamReader xmlr) {
		if (xmlr.hasName()) {
			valuesAndAttributesContainer.pop();
		}
	}

	@Override
	protected void nextStartElement(XMLStreamReader xmlr) {
		if (xmlr.hasName()) {
			valuesAndAttributesContainer.push(xmlr.getName());
		}
		processAttributes(xmlr);
	}

	protected void processAttribute(XMLStreamReader xmlr, int index) {
		String localName = xmlr.getAttributeLocalName(index);
		String value = xmlr.getAttributeValue(index);
		valuesAndAttributesContainer.setAttribute(localName, value);

	}

	private void readAnnotations(Class<?>[] classes) {
		AnnotationProcessor p = new AnnotationProcessor();
		p.processClasses(valuesAndAttributesContainer, classes);
	}

}
