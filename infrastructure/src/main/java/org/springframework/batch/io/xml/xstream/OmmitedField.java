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
 * Defines a field which shouldn't be serialized. To omit a field you must
 * always provide the declaring type and not necessarily the type that is
 * converted.
 * @author peter.zozom
 * 
 */
public class OmmitedField {
	private String type;

	private String fieldName;

	/**
	 * @return field which should be ommited
	 */
	protected String getFieldName() {
		return fieldName;
	}

	/**
	 * Set field which should be ommited.
	 * @param fieldName field which should be ommited
	 */
	protected void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return declaring type of the ommited field
	 */
	protected String getType() {
		return type;
	}

	/**
	 * Set declaring type of the ommited field.
	 * @param type declaring type of the ommited field
	 */
	protected void setType(String type) {
		this.type = type;
	}
}
