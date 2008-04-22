/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.database.support;

import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * Factory for creating {@link DataFieldMaxValueIncrementer} implementations
 * based upon a provided string.
 * 
 * @author Lucas Ward
 *
 */
public interface DataFieldMaxValueIncrementerFactory {

	/**
	 * Return the {@link DataFieldMaxValueIncrementer} for the provided database type.
	 * 
	 * @param databaseType string represented database type
	 * @param incrementerName incrementer name to create. In many cases this may be the
	 *  sequence name
	 * @return incrementer
	 * @throws IllegalArgumentException if databaseType is invalid type, or incrementerName
	 * is null.
	 */
	public DataFieldMaxValueIncrementer getIncrementer(String databaseType, String incrementerName);
	
	/**
	 * Returns boolean indicated whether or not the provided string is supported by this
	 * factory.
	 */
	public boolean isSupportedIncrementerType(String databaseType);

	/**
	 * Returns the list of supported database incrementer types
	 */
	public String[] getSupportedIncrementerTypes();
}
