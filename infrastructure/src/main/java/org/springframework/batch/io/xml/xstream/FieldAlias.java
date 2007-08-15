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
 * Represents an alias for a field name.
 * @author peter.zozom
 */
public class FieldAlias {
	private String aliasName;

	private String type;

	private String fieldName;

	/**
	 * @return field alias
	 */
	public String getAliasName() {
		return aliasName;
	}

	/**
	 * Set field alias name.
	 * @param aliasName the alias itself
	 */
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	/**
	 * @return field name to be aliased
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Set the name of the field to be aliased.
	 * @param fieldName the name of the field to be aliased
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the type that declares the field
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type that declares the field.
	 * @param type the type that declares the field
	 */
	public void setType(String type) {
		this.type = type;
	}
}
