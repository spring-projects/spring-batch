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

import org.springframework.batch.item.xmlpathreader.path.PathEntry;

/**
 * A Attribute sets a property of a class to a object value
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public interface Attribute extends PathEntry {

	/**
	 * set an attribute/property Value
	 * 
	 * @param objValue the object to witch the attribute is set
	 */
	void setValue(Object objValue);

	/**
	 * settable from a String
	 * 
	 * @return is this attribute a string related attribute
	 */
	public boolean isSetableFromString();
}
