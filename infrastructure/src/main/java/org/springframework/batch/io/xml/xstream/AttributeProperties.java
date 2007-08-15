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
 * Defines which XML attribute to use for a field or a specific type.
 * @author peter.zozom
 */
public class AttributeProperties {

	private String type;

	private String fieldName;

	/**
	 * @return the field name which will be rendered as XML attribute
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Set the field to be rendered as XML attribute
	 * @param fieldName the name of the field
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set type to be used for XML attribute.
	 * @param type the name of the type to be rendered as XML attribute
	 */
	public void setType(String type) {
		this.type = type;
	}
}
