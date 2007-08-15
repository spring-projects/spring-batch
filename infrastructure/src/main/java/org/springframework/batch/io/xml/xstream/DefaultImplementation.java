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
 * Defines default implementation of a class which should associated with an
 * object. Whenever XStream encounters an instance of this type, it will use the
 * default implementation instead. For example, java.util.ArrayList is the
 * default implementation of java.util.List.
 * @author peter.zozom
 */
public class DefaultImplementation {

	private String defaultImpl;

	private String type;

	/**
	 * @return class name of the default implementation
	 */
	public String getDefaultImpl() {
		return defaultImpl;
	}

	/**
	 * Set the class name of the default implementation which should be
	 * associated with ofType.
	 * @param defaultImpl class name of the default implementation
	 */
	public void setDefaultImpl(String defaultImpl) {
		this.defaultImpl = defaultImpl;
	}

	/**
	 * @return type name associated with default implementation
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set type name which should be associated with default implementation.
	 * @param ofType type name
	 */
	public void setType(String ofType) {
		this.type = ofType;
	}
}
