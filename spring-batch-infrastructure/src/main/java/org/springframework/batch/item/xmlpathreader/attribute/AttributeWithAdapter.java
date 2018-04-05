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

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.util.Assert;

/**
 * An Attribute that sets a property where the value is first converted from a string value. The string value is
 * translated with an XmlAdapter adapter.
 * @author Thomas Nill
 * @since 4.0.1
 * @see Attribute
 * @see XmlAdapter
 *
 */
// @FunctionalInterface
public class AttributeWithAdapter implements Attribute {
	private static final Logger log = LoggerFactory.getLogger(AttributeWithAdapter.class);

	private XmlAdapter<String, ?> adapter;

	private Attribute delegate = null;

	/**
	 * Constructor
	 * 
	 * @param delegate the adapter that is used to convert a String to an Object
	 * @param adapter the adapter for this class
	 */
	public AttributeWithAdapter(Attribute delegate, XmlAdapter<String, ?> adapter) {
		super();
		Assert.notNull(delegate, "Attribute should not be null");
		Assert.notNull(adapter, "Adapter should not be null");
		this.delegate = delegate;
		this.adapter = adapter;
	}

	@Override
	public void setValue(Object objValue) {
		log.debug("setValue {}", delegate);
		Object o = "";
		try {
			o = adapter.unmarshal(objValue.toString());
			delegate.setValue(o);
		}
		catch (Exception e) {
			Messages.throwReaderRuntimeException(e, "Runtime.NOT_APPLICABLE", "method", "type", "value", o.getClass());
		}
	}

	@Override
	public XmlElementPath getPath() {
		return delegate.getPath();
	}

	@Override
	public boolean isSetableFromString() {
		return true;
	}

}
