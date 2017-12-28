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
import org.springframework.batch.item.xmlpathreader.path.PathEntryContainer;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.util.Assert;

/**
 * The AttributeContainer is a Collection of Attribute entries
 * @author Thomas Nill
 * @since 4.0.1
 * @see Attribute
 *
 */
public class AttributeContainer extends PathEntryContainer<Attribute> {
	private static final Logger log = LoggerFactory.getLogger(AttributeContainer.class);

	/**
	 * constructor of parent class
	 */
	public AttributeContainer() {
		super();
	}

	/**
	 * call a Attribute setValue action
	 * 
	 * @param path the path to the attribute that is used
	 * @param value the object to witch the attribute is set
	 */
	public void setValue(XmlElementPath path, String value) {
		Assert.notNull(path, "Path should not be null");

		log.debug(" setValue auf Path {} to {} ", path, value);
		Attribute attribute = searchTheBestMatchingEntity(path);
		if (attribute != null && attribute.isSetableFromString()) {
			log.debug(" find {} ", attribute.getPath());
			attribute.setValue(value);
		}
	}

}
