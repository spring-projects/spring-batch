/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.ldif;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ldif.support.MappingLdifReaderTestSupport;
import org.springframework.ldap.core.LdapAttributes;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Banseok Kim
 */
class MappingLdifReaderTests extends MappingLdifReaderTestSupport {

	@Test
	void readRecordsMappingToDnStringUsingSetters() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReader<>(ldifResource);
		reader.setName("ldif");
		reader.setRecordMapper(new StringMapper());
		reader.setStrict(true);
		reader.afterPropertiesSet();

		verify(reader, expectedDns());
	}

	@Test
	void missingResourceInStrictModeShouldFailOnOpen() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReader<>(missingResource());
		reader.setName("ldif");
		reader.setRecordMapper(new StringMapper());
		reader.setStrict(true);
		reader.afterPropertiesSet();

		assertThrows(Exception.class, () -> reader.open(new ExecutionContext()));
	}

	private static class StringMapper implements RecordMapper<String> {

		@Override
		public @Nullable String mapRecord(@Nullable LdapAttributes attributes) {
			if (attributes == null) {
				return null;
			}
			return attributes.getName().toString();
		}

	}

}
