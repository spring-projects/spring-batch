/*
 * Copyright 2005-2014 the original author or authors.
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
package org.springframework.batch.item.ldif;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.ldap.core.LdapAttributes;

/**
 * The {@link LdifAggregator LdifAggregator} object is an implementation of the {@link org.springframework.batch.item.file.transform.LineAggregator LineAggregator}
 * interface for use with a {@link org.springframework.batch.item.file.FlatFileItemWriter FlatFileItemWriter} to write LDIF records to a file.
 *
 * @author Keith Barlow
 *
 */
public class LdifAggregator implements LineAggregator<LdapAttributes> {

	/**
	 * Returns a {@link java.lang.String String} containing a properly formated LDIF.
	 *
	 * @param item LdapAttributes object to convert to string.
	 * @return string representation of the object LDIF format (in accordance with RFC 2849).
	 */
	public String aggregate(LdapAttributes item) {
		return item.toString();
	}

}