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
 * Represents mapping of a class to a shorter name to be used in XML elements.
 * @author peter.zozom
 */
public class ClassAlias {

	private String name;

	private String type;

	private String defaultImplementation;

	/**
	 * @return the default implementation of the type
	 */
	public String getDefaultImplementation() {
		return defaultImplementation;
	}

	/**
	 * Set default implementation of type to use.
	 * @param defaultImplementation Default implementation of type to use if no
	 * other specified.
	 */
	public void setDefaultImplementation(String defaultImplementation) {
		this.defaultImplementation = defaultImplementation;
	}

	/**
	 * @return short name for the type
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set short name.
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return aliased type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set aliased type.
	 * @param type type to be aliased
	 */
	public void setType(String type) {
		this.type = type;
	}
}
