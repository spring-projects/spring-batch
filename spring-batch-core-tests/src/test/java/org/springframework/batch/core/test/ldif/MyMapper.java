/*
 * Copyright 2005-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.ldif;

import org.springframework.batch.item.ldif.RecordMapper;
import org.springframework.lang.Nullable;
import org.springframework.ldap.core.LdapAttributes;

/**
 * This default implementation simply returns the LdapAttributes object and is only intended for test.  As its not required
 * to return an object of a specific type to make the MappingLdifReader implementation work, this basic setting is sufficient
 * to demonstrate its function.
 *
 * @author Keith Barlow
 *
 */
public class MyMapper implements RecordMapper<LdapAttributes> {

	@Nullable
	public LdapAttributes mapRecord(LdapAttributes attributes) {
		return attributes;
	}

}
