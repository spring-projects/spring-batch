/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.io.xml.xstream;

/**
 * Represents alias for an attribute.
 * @author peter.zozom
 */
public class AttributeAlias {

	private String alias;

	private String attributeName;

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Set alias for attribute
	 * @param alias the alias itself
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * @return the attribute name
	 */
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * Set the attribute name.
	 * @param attributeName the name of the attribute
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
}
