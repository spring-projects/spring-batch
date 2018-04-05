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
package org.springframework.batch.item.xmlpathreader.attribute;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPathEntry;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.util.Assert;

/**
 * An Attribute that sets a property of a class to a value with a Method
 * @author Thomas Nill
 * @since 4.0.1
 * @see Method
 */
public class MethodAttribute extends XmlElementPathEntry implements Attribute {
	private static final Logger log = LoggerFactory.getLogger(MethodAttribute.class);

	protected Method method;

	protected Value value;

	/**
	 * Constructor
	 * 
	 * @param path the path to the attribute
	 * @param method the setter method that set the attribute
	 * @param value the value that contains the object that will be set
	 */
	public MethodAttribute(XmlElementPath path, Method method, Value value) {
		super(path);
		Assert.notNull(method, "Method should not be null");
		Assert.notNull(value, "Value should not be null");
		this.method = method;
		this.value = value;
	}

	@Override
	public void setValue(Object objValue) {
		try {
			log.debug("setMethod {} of Value {} to {}", method, value.getPath(), objValue);
			method.invoke(value.getValue(), objValue);
		}
		catch (Exception e) {
			if (value.getValue() == null) {
				Messages.throwReaderRuntimeException(e, "Runtime.NOT_APPLICABLE", method.getName(),
						method.getParameterTypes()[0].getTypeName(), "null", "unknown");
			}
			else {
				Messages.throwReaderRuntimeException(e, "Runtime.NOT_APPLICABLE", method.getName(),
						method.getParameterTypes()[0].getTypeName(), value.getValue(), value.getValue().getClass()
								.getName());
			}
		}
	}

	/**
	 * setable from a String
	 * 
	 * @return is able to set from a String
	 */
	@Override
	public boolean isSetableFromString() {
		return method.getParameterTypes()[0].equals(String.class);
	}

}
