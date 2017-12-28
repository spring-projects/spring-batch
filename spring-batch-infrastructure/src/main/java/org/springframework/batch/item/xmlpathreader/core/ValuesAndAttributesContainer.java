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

import java.lang.reflect.Method;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.batch.item.xmlpathreader.adapters.AdapterMap;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPaths;
import org.springframework.batch.item.xmlpathreader.attribute.Attribute;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithAdapter;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithValue;
import org.springframework.batch.item.xmlpathreader.attribute.MethodAttribute;
import org.springframework.batch.item.xmlpathreader.attribute.Setter;
import org.springframework.batch.item.xmlpathreader.attribute.SetterAttribute;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.value.ClassValue;
import org.springframework.batch.item.xmlpathreader.value.Creator;
import org.springframework.batch.item.xmlpathreader.value.CreatorValue;
import org.springframework.batch.item.xmlpathreader.value.CurrentObject;
import org.springframework.batch.item.xmlpathreader.value.SimpleCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.StackCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.StaticMethodValue;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;
import org.springframework.util.Assert;

/**
 * A Container of the Element Names in a XML document There a creation methods for the different types of Value and @Attribute
 * classes.
 * @author Thomas Nill
 * @since 4.0.1
 * @see Value
 * @see Attribute
 */

public class ValuesAndAttributesContainer extends ValuesAndAttributesBag {

	/**
	 * constructor with a empty configuration
	 * 
	 * @param current the global {@link CurrentObject}
	 */
	public ValuesAndAttributesContainer(CurrentObject current) {
		this(current, new ValueContainer());
	}

	/**
	 * full configured with a NamedActionMap
	 * 
	 * @param current the global {@link CurrentObject}
	 * @param map the container for the values
	 */
	public ValuesAndAttributesContainer(CurrentObject current, ValueContainer map) {
		super(current, map);
	}

	/**
	 * Add the creation of a class instance to a path of XML Elements
	 * 
	 * @param valuePath path of XML Elements
	 * @param clazz class of the generated instance
	 * @param creator the {@link Creator} that is used
	 * @return a {@link Value}
	 */
	public Value addValue(XmlElementPath valuePath, Class<?> clazz, Creator creator) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);

		Value value = new CreatorValue(valuePath, clazz, getCurrent(), createCurrentObject(valuePath), creator);
		addValue(value);
		return value;
	}

	/**
	 * Add the creation of a class instance to a path of XML Elements
	 * 
	 * @param valuePath path of XML Elements
	 * @param clazz class of the generated instance
	 * @return a {@link Value}
	 */

	public Value addValue(XmlElementPath valuePath, Class<?> clazz) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);

		Value value = new ClassValue(valuePath, clazz, getCurrent(), createCurrentObject(valuePath));
		addValue(value);
		return value;
	}

	private CurrentObject createCurrentObject(XmlElementPath name) {
		return name.isAbsolut() ? new SimpleCurrentObject() : new StackCurrentObject();
	}

	/**
	 * Add the creation of a class instance to a path of XML Elements
	 * 
	 * @param valuePath path of XML Elements
	 * @param clazz class of the generated instance
	 * @param methodName the name of the method, of the value object
	 * @return a {@link Value}
	 */

	public Value addValue(XmlElementPath valuePath, Class<?> clazz, String methodName) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(clazz, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.hasText(methodName, THE_METHOD_NAME_SHOULD_NOT_BE_EMPTY);

		Value value = new StaticMethodValue(valuePath, clazz, getCurrent(), createCurrentObject(valuePath), methodName);
		addValue(value);
		return value;
	}

	/**
	 * Add the setXXXX setter to a path of XML Elements
	 * 
	 * @param valuePath the path of XML elements to the object instance, that will be set
	 * @param attributePath the relative path of XML elements to a text-value, for the value attribute of the setter
	 * @param field the name of the setter Method
	 * @return an {@link Attribute}
	 */
	public Attribute addRelativAttribute(XmlElementPath valuePath, XmlElementPath attributePath, String field) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.hasText(field, THE_FIELD_SHOULD_NOT_BE_EMPTY);

		return addAttribute(valuePath, valuePath.concat(attributePath), field);
	}

	/**
	 * Add the setXXXX setter to a path of XML Elements
	 * 
	 * @param valuePath the path of XML elements to the object instance, that will be set
	 * @param attributePath the absolute path of XML elements to a text-value, for the attribute parameter
	 * @param field (the name of the setter Method)
	 * @return an {@link Attribute}
	 */
	public Attribute addAttribute(XmlElementPath valuePath, XmlElementPath attributePath, String field) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.hasText(field, THE_FIELD_SHOULD_NOT_BE_EMPTY);

		if (attributePath.startsWith(valuePath)) {
			Value value = checkArguments(valuePath, attributePath, field);
			Attribute attribute = createAttribute(value, attributePath, field);
			addAttribute(attribute);
			return attribute;
		}
		return null;
	}

	/**
	 * create SetAction from a Setter
	 * 
	 * @param <T> the type of the value
	 * @param <V> the type of the attribute
	 * @param value the {@link Value} that is set
	 * @param attributePath the path to the element with the text for the attribute
	 * @param handle the {@link Setter} that is used
	 * @return an {@link Attribute}
	 */
	public <T, V> Attribute addAttribute(Value value, XmlElementPath attributePath, Setter<T, V> handle) {
		Assert.notNull(value, THE_VALUE_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.notNull(handle, THE_SETTER_SHOULD_NOT_BE_NULL);

		Attribute attribute = new SetterAttribute(attributePath, handle, value);
		addAttribute(attribute);
		return attribute;
	}

	/**
	 * create SetAction from a Setter
	 * 
	 * @param <T> the type of the value
	 * @param <V> the type of the attribute
	 * @param valuePath the path of XML elements to the object instance, that will be set
	 * @param attributePath the relative path of XML elements to a text-value, for the attribute parameter
	 * @param handle the {@link Setter} that is used
	 * @return an {@link Attribute}
	 */
	public <T, V> Attribute addAttribute(XmlElementPath valuePath, XmlElementPath attributePath, Setter<T, V> handle) {
		Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
		Assert.notNull(attributePath, THE_CLASS_SHOULD_NOT_BE_NULL);
		Assert.notNull(handle, THE_SETTER_SHOULD_NOT_BE_NULL);
		Value value = checkValue(valuePath);
		return addAttribute(value, valuePath.concat(attributePath), handle);
	}

	/**
	 * create a Attribute form a method name
	 * 
	 * @param value the [@link Value} that is set
	 * @param valuePath the path to the value
	 * @param fieldName the name of the property that will be set
	 * 
	 * @return an {@link Attribute}
	 */
	public Attribute createAttribute(Value value, XmlElementPath valuePath, String fieldName) {
		try {
			Assert.notNull(value, THE_VALUE_SHOULD_NOT_BE_NULL);
			Assert.notNull(valuePath, THE_PATH_SHOULD_NOT_BE_NULL);
			Assert.hasText(fieldName, THE_FIELD_SHOULD_NOT_BE_EMPTY);

			String methodName = "set" + fieldName;
			Method method = searchTheMethod(value.getClazz(), methodName, String.class);
			Class<?> targetClass = method.getParameterTypes()[0];
			if (targetClass.isAnnotationPresent(XmlPath.class)) {
				XmlPath xpath = targetClass.getAnnotation(XmlPath.class);
				return createSetFromValue(value, valuePath, new XmlElementPath(xpath.path()), method);
			}
			if (targetClass.equals(String.class)) {
				return createAttribute(value, valuePath, method);
			}
			else {
				return createAttribute(value, valuePath, method, AdapterMap.getAdapter(targetClass));
			}

		}
		catch (Exception e) {
			Messages.throwReaderRuntimeException(e, "Runtime.NOT_METHOD", value.getClazz().getName(), fieldName);
			return null;
		}
	}

	/**
	 * create SetAction from a Method
	 * 
	 * @param handle
	 * @return an {@link Attribute}
	 */

	private Attribute createAttribute(Value value, XmlElementPath rValuePath, Method handle) {
		return new MethodAttribute(rValuePath, handle, value);
	}

	private Attribute createSetFromValue(Value value, XmlElementPath setterPath, XmlElementPath valuePath, Method handle) {
		log.debug(" createSetFromValuAction Attribute {} From {} ", setterPath, valuePath);
		AttributeWithValue attributeWithValue = new AttributeWithValue(setterPath, valuePath, getValueMap(),
				getAttributeMap());
		addAttributeWithValue(attributeWithValue);
		return new MethodAttribute(setterPath, handle, value);
	}

	/**
	 * create SetAction from a Method
	 * 
	 * @param handle
	 * @param adapter
	 * @return an {@link Attribute}
	 */
	// NOSONAR because this method is USED
	@SuppressWarnings("squid:UnusedPrivateMethod")
	private Attribute createAttribute(Value value, XmlElementPath rValuePath, Method handle,
			XmlAdapter<String, ?> adapter) {
		Attribute delegate = new MethodAttribute(rValuePath, handle, value);
		return new AttributeWithAdapter(delegate, adapter);
	}

	/**
	 * search a method
	 * 
	 * 
	 * @param clazz
	 * @param name
	 * @param targetClass
	 * @return a Method
	 * @throws Exception
	 */
	// NOSONAR because this method is USED
	@SuppressWarnings("squid:UnusedPrivateMethod")
	private Method searchTheMethod(Class<?> clazz, String name, Class<?> targetClass) {
		Method bestMethod = null;
		Method usableMethod = null;
		for (Method method : clazz.getMethods()) {
			if (name.equals(method.getName()) && method.getParameterCount() == 1) {
				Class<?> parameterType = method.getParameterTypes()[0];
				if (parameterType == targetClass) {
					bestMethod = method;
				}
				if (AdapterMap.hasAdapterForClass(parameterType)) {
					usableMethod = method;
				}
				if (parameterType.isAnnotationPresent(XmlPath.class)
						|| parameterType.isAnnotationPresent(XmlPaths.class)) {
					usableMethod = method;
				}
			}
		}
		if (bestMethod != null) {
			return bestMethod;
		}
		return usableMethod;
	}

}
