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

package org.springframework.batch.item.xmlpathreader.core;

import java.util.ArrayDeque;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.attribute.Attribute;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeContainer;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithValue;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithValueContainer;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.value.CurrentObject;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;
import org.springframework.util.Assert;

/**
 * A bag with Values and Attributes and a CurrentObject
 * <p>
 * If the cursor of a stax-StaxXmlPathReader is at a position in the document then the started elements form a stack of
 * QName element names that are currently open.
 * <p>
 * At the starttag of a element, the element QName is pushed at the top of the stack. At the endtag the element is
 * closed and disappears with a pop from the stack
 * <p>
 * Attributes nodes push and pop the attribute name with the prefix {@literal @} to the stack.
 * <p>
 * The path of the container is the concatenated list of element names, with a {@literal /} as a delimiter
 * <p>
 * at the time of push or pop a action will bee called that is connected to the current path. If no action exists,
 * nothing happens.
 * <p>
 * The currentObject hold the top object that is completed after a endtag.
 * @author Thomas Nill
 * @since 4.0.1
 * @see Value
 * @see CurrentObject
 * @see QName
 */

public class ValuesAndAttributesBag extends ArrayDeque<QName> {

	protected static final String THE_VALUE_SHOULD_NOT_BE_NULL = "The value should not be null";

	protected static final String THE_FIELD_SHOULD_NOT_BE_EMPTY = "The field should not be empty";

	protected static final String THE_METHOD_NAME_SHOULD_NOT_BE_EMPTY = "The methodName should not be empty";

	protected static final String THE_CLASS_SHOULD_NOT_BE_NULL = "The class should not be null";

	private static final String THE_ITEM_SHOULD_NOT_BE_EMPTY = "The item should not be empty";

	protected static final String THE_PATH_SHOULD_NOT_BE_NULL = "The path should not be null";

	protected static final String THE_MAP_SHOULD_NOT_BE_NULL = "The map should not be null";

	protected static final String THE_CURRENT_SHOULD_NOT_BE_NULL = "The current should not be null";

	protected static final String THE_SETTER_SHOULD_NOT_BE_NULL = "The setter should not be null";

	protected static final Logger log = LoggerFactory.getLogger(ValuesAndAttributesContainer.class);

	private static final long serialVersionUID = 773837341597279034L;

	private CurrentObject current;

	private ValueContainer valueContainer = new ValueContainer();

	private AttributeContainer attributeContainer = new AttributeContainer();

	private AttributeWithValueContainer attributeWithValueContainer = new AttributeWithValueContainer();

	/**
	 * full configured with a ValueContainer
	 * 
	 * @param current the global {@link CurrentObject}
	 * @param valueContainer the container for the values
	 */
	public ValuesAndAttributesBag(CurrentObject current, ValueContainer valueContainer) {
		super();
		Assert.notNull(current, THE_CURRENT_SHOULD_NOT_BE_NULL);
		Assert.notNull(valueContainer, THE_MAP_SHOULD_NOT_BE_NULL);

		this.current = current;
		this.valueContainer = valueContainer;
	}

	public CurrentObject getCurrent() {
		return current;
	}

	protected ValueContainer getValueMap() {
		return valueContainer;
	}

	protected AttributeContainer getAttributeMap() {
		return attributeContainer;
	}

	/**
	 * create a path, that represent the current state of the stack, as a String, with delimiter {@literal /}
	 * 
	 * @return the current path of the {@link QName}s
	 */
	public XmlElementPath getCurrentPath() {
		Object[] qObjects = this.toArray();
		QName[] qNames = new QName[qObjects.length];
		for (int i = 0; i < qNames.length; i++) {
			// exchange the direction
			qNames[qNames.length - 1 - i] = (QName) qObjects[i];
		}
		return new XmlElementPath(qNames);
	}

	/**
	 * Add a @{Value} to a path of XML Elements
	 * 
	 * @param value the value
	 */

	public void addValue(Value value) {
		this.valueContainer.put(value);
	}

	/**
	 * Add a @{Attribute} to a path of XML Elements
	 * 
	 * @param attribute the attribute
	 */
	public void addAttribute(Attribute attribute) {
		attributeContainer.put(attribute);
	}

	protected void addAttributeWithValue(AttributeWithValue attributeWithValue) {
		attributeWithValueContainer.put(attributeWithValue);
	}

	/**
	 * push a element name on the stack, calls a push method of a Value if it exists
	 * 
	 */
	@Override
	public void push(QName item) {
		Assert.notNull(item, THE_ITEM_SHOULD_NOT_BE_EMPTY);

		log.debug("Push a Tag " + item);
		super.push(item);
		XmlElementPath path = getCurrentPath();
		valueContainer.push(path);
	}

	/**
	 * pop a element name on the stack, calls a pop method of a Value if it exists
	 * 
	 */
	@Override
	public QName pop() {
		XmlElementPath path = getCurrentPath();

		setFromValue(path);

		QName erg = super.pop();
		valueContainer.pop(path);
		return erg;
	}

	/**
	 * Created Object for a Class, perhaps it is not fully instantiated
	 *
	 * @param path the path to the {@link Value}
	 * @return the object of the {@link Value}
	 */
	public Object getValueObject(XmlElementPath path) {
		Assert.notNull(path, THE_PATH_SHOULD_NOT_BE_NULL);

		Value value = valueContainer.searchTheBestMatchingEntity(path);
		if (value != null) {
			return value.getValue();
		}
		return null;
	}

	/**
	 * Created Object for a Class, perhaps it is not fully instantiated with a Exception if the value or Object does not
	 * exist
	 * 
	 * @param path the path to the {@link Value}
	 * @return the object of the {@link Value}
	 */
	public Object getValueObjectWithException(XmlElementPath path) {
		Assert.notNull(path, THE_PATH_SHOULD_NOT_BE_NULL);

		Object obj = getValueObject(path);
		if (obj == null) {
			throw new IllegalArgumentException("Aa value for " + path + " does not exist ");
		}
		return obj;
	}

	/**
	 * Call the {@link AttributeWithValue} to the path
	 * 
	 * @param path the path to the attribute
	 */
	public void setFromValue(XmlElementPath path) {
		Assert.notNull(path, THE_PATH_SHOULD_NOT_BE_NULL);

		log.debug("at pop  {} ", path);
		attributeWithValueContainer.setValue(path);
	}

	/**
	 * call a setter for a attribute in a XML document
	 * 
	 * @param name the name of the XML attribute
	 * @param value the object to that the {@link Attribute} will be set
	 */
	public void setAttribute(String name, String value) {
		Assert.hasText(name, THE_ITEM_SHOULD_NOT_BE_EMPTY);

		this.push(new QName("@" + name));
		setText(value);
		this.pop();
	}

	/**
	 * call a setter from a text node
	 * 
	 * @param value the object to that the {@link Attribute} will be set
	 */
	public void setText(String value) {
		XmlElementPath path = getCurrentPath();
		attributeContainer.setValue(path, value);
	}

	protected Value checkArguments(XmlElementPath valuePath, XmlElementPath attributePath, String field) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.hasText(field, THE_FIELD_SHOULD_NOT_BE_EMPTY);

		checkName(valuePath);
		return checkValue(valuePath);
	}

	private Object checkName(XmlElementPath valueName) {

		Object o = valueContainer.searchTheBestMatchingEntity(valueName);
		if (o == null) {
			throw new IllegalArgumentException("Value " + valueName + " ist nicht vorhanden");
		}
		return o;
	}

	protected Value checkValue(XmlElementPath valueName) {
		return valueContainer.searchTheBestMatchingEntity(valueName);
	}
}