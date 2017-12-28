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
 * The AttributeWithValueContainer contains AttributeWithValue
 * 
 * @author Thomas Nill
 * @since 4.0.1
 * @see AttributeWithValue
 */
public class AttributeWithValueContainer extends PathEntryContainer<AttributeWithValue> {
	private static final Logger log = LoggerFactory.getLogger(AttributeWithValueContainer.class);

	/**
	 * constructor of parent class
	 */
	public AttributeWithValueContainer() {
		super();
	}

	@Override
	public AttributeWithValue searchTheBestMatchingEntity(XmlElementPath path) {
		Assert.notNull(path, "The path should not be null");
		log.debug("searchTheBestMatchingEntity for the path {} ", path);
		AttributeWithValue bestMatch = null;
		for (AttributeWithValue e : this) {
			if (e.getPath().compare(path)) {
				log.debug("better element {} ", e);
				bestMatch = e;
			}
		}
		return bestMatch;
	}

	/**
	 * Call the setValue() method of the best matching AttributeWithValue.
	 * 
	 * @param path the path to the attribut that is set
	 */
	public void setValue(XmlElementPath path) {
		Assert.notNull(path, "Path should not be null");
		AttributeWithValue s = searchTheBestMatchingEntity(path);
		if (s != null) {
			s.setValue();
		}
	}

}
