/*
 * Copyright 2026 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.ldif.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ldif.MappingLdifReader;
import org.springframework.ldap.core.LdapAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Banseok Kim
 */
public abstract class MappingLdifReaderTestSupport extends LdifTestFixtures {

	protected void verify(MappingLdifReader<String> reader, List<String> expectedDns) throws Exception {
		reader.open(new ExecutionContext());
		try {
			List<String> actual = new ArrayList<>();
			String item;
			while ((item = reader.read()) != null) {
				actual.add(item);
			}
			assertThat(actual).containsExactlyElementsOf(expectedDns);
		}
		finally {
			reader.close();
		}
	}

}
