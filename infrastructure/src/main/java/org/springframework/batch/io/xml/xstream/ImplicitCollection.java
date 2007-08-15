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
 * Represents implicit collection definition. Implicit collection is used for:
 * <ul>
 * <li>any unmapped xml tag</li>
 * <li>or all items of the given itemType</li>
 * <li>or all items of the given element name defined by itemFieldName</li>
 * </ul>
 * 
 * @author peter.zozom
 */
public class ImplicitCollection {
	private String ownerType;

	private String fieldName;

	private String itemFieldName;

	private String itemType;

	/**
	 * @return name of the field in the ownerType
	 */
	protected String getFieldName() {
		return fieldName;
	}

	/**
	 * Set name of the field in the owner class. This field must be an
	 * <code>java.util.ArrayList</code>.
	 * @param fieldName name of the field in the ownerType
	 */
	protected void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return element name of the implicit collection
	 */
	protected String getItemFieldName() {
		return itemFieldName;
	}

	/**
	 * Set element name of the implicit collection.
	 * @param itemFieldName element name of the implicit collection
	 */
	protected void setItemFieldName(String itemFieldName) {
		this.itemFieldName = itemFieldName;
	}

	/**
	 * @return type of the items to be part of this collection
	 */
	protected String getItemType() {
		return itemType;
	}

	/**
	 * Set yype of the items to be part of this collection (aliased with
	 * ItemFieldName, if provided).
	 * @param itemType type of the items to be part of this collection
	 */
	protected void setItemType(String itemType) {
		this.itemType = itemType;
	}

	/**
	 * @return class owning the implicit collection
	 */
	protected String getOwnerType() {
		return ownerType;
	}

	/**
	 * Set class that owns implicit collection.
	 * @param ownerType class owning the implicit collection
	 */
	protected void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}
}
