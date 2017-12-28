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
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.util.Assert;

/**
 * The AttributeWithValuePath is a special XmlElementPath. This is a path to an Attribute together with a path to a
 * Value
 * @author Thomas Nill
 * @since 4.0.1
 * @see Attribute
 * @see XmlElementPath
 * @see org.springframework.batch.item.xmlpathreader.value.Value
 * 
 *
 */
public class AttributeWithValuePath extends XmlElementPath {
	private static final Logger log = LoggerFactory.getLogger(AttributeWithValuePath.class);

	private XmlElementPath valuePath;

	/**
	 * Constructor
	 * 
	 * @param attributePath the path to the attribute that set the a {@link Value}
	 * @param valuePath the path to the value with the parameter object to with the attribute will be set.
	 */
	public AttributeWithValuePath(XmlElementPath attributePath, XmlElementPath valuePath) {
		super(attributePath);
		Assert.notNull(attributePath, "setterPath should not be null");
		Assert.notNull(valuePath, "valuePath should not be null");

		this.valuePath = valuePath;
	}

	@Override
	public boolean compare(XmlElementPath path) {
		Assert.notNull(path, "path should not be null");

		log.debug("compare {} and {}", this, path);
		String pathString = path.getPath();
		if (pathString.endsWith(valuePath.getPath())) {
			log.debug("ends with {} ", valuePath.getPath());
			if (pathString.endsWith(getPath())) {
				log.debug("and {} ends with {}", pathString, getPath());
				return true;
			}
			else {
				String concat = this.getPath() + '/' + valuePath.getPath();
				boolean ok = pathString.endsWith(concat);
				if (ok) {
					log.debug("and {} end with {}", pathString, concat);
				}
				else {
					log.debug("and {} does not end with {}", pathString, concat);
				}
				return ok;
			}
		}
		else {
			log.debug("do not ends with {} ", valuePath.getPath());
		}
		return false;
	}

}
