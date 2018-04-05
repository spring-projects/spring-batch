/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");c
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

package org.springframework.batch.item.xmlpathreader.attribute;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPathEntry;
import org.springframework.batch.item.xmlpathreader.value.CurrentObject;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;
import org.springframework.util.Assert;

/**
 * Set a property of a class with that has a XmlPath annotation. The property is an object of another class that has a
 * XmlPath annotation too.
 * @author Thomas Nill
 * @since 4.0.1
 * @see XmlPath
 */

public class AttributeWithValue extends XmlElementPathEntry {

	private XmlElementPath valuePath;

	private XmlElementPath attributePath;

	private ValueContainer valueContainer;

	private AttributeContainer attributeContainer;

	private Value value;

	private Attribute attribute;

	/**
	 * Constructor
	 * 
	 * @param attributePath the path to the {@link Attribute}
	 * @param valuePath the path to the {@link Value} with the parameter object to that the attribute will be set
	 * @param valueContainer the container that holds the values
	 * @param attributeContainer the container that holds the attributes
	 */
	public AttributeWithValue(XmlElementPath attributePath, XmlElementPath valuePath, ValueContainer valueContainer,
			AttributeContainer attributeContainer) {
		super(new AttributeWithValuePath(attributePath, valuePath));
		Assert.notNull(valueContainer, "ValueContainer should not be null");
		Assert.notNull(attributeContainer, "AttributeContainer should not be null");

		this.valueContainer = valueContainer;
		this.attributeContainer = attributeContainer;
		this.valuePath = valuePath;
		this.attributePath = attributePath;
	}

	/**
	 * set the value of the property
	 */
	public void setValue() {
		searchTheValueAndAttributeInTheContainer();
		if (value != null && attribute != null) {
			setTheAttributFromTheValue();
		}
	}

	private void setTheAttributFromTheValue() {
		CurrentObject current = value.getCurrent();
		Object o = current.popCurrentObject();
		attribute.setValue(o);
		current.pushCurrentObject(o);
	}

	private void searchTheValueAndAttributeInTheContainer() {
		if (value == null || attribute == null) {
			value = valueContainer.searchTheBestMatchingEntity(valuePath);
			attribute = attributeContainer.searchTheBestMatchingEntity(attributePath);
		}
	}
}
