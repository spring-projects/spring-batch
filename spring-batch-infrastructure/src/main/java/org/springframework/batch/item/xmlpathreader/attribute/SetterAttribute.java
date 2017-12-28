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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPathEntry;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.util.Assert;

/**
 * An Attribute that sets a property of a class to a value with a Setter
 * @author Thomas Nill
 * @since 4.0.1
 * @see Setter
 */
public class SetterAttribute extends XmlElementPathEntry implements Attribute {
	private static final Logger log = LoggerFactory.getLogger(SetterAttribute.class);

	protected Setter setter;

	protected Value value;

	/**
	 * Constructor
	 * 
	 * @param path the path to the {@link Attribute}
	 * @param setter the {@link Setter} that is used
	 * @param value the {@link Value} that ist set with the {@link Setter}
	 */
	public SetterAttribute(XmlElementPath path, Setter setter, Value value) {
		super(path);
		Assert.notNull(setter, "Creator should not be null");
		Assert.notNull(value, "Value should not be null");
		this.setter = setter;
		this.value = value;
	}

	public void setValue(Object objValue) {
		try {
			log.debug("setMethod {} of Value {} to {}", setter, value.getPath(), objValue);
			setter.setValue(value.getValue(), objValue);
		}
		catch (Exception e) {
			if (value.getValue() == null) {
				Messages.throwReaderRuntimeException(e, "Runtime.SET_ERROR", setter.toString(), "null", "unknown");
			}
			else {
				Messages.throwReaderRuntimeException(e, "Runtime.SET_ERROR", setter.toString(), value.getValue()
						.getClass().getName());
			}
		}
	}

	/**
	 * settable from a String
	 * 
	 * @return is the Value set able from a String
	 */
	public boolean isSetableFromString() {
		return true;
	}

}
