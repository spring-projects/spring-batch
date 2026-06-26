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

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ldif.support.LdifReaderTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Banseok Kim
 */
class LdifReaderTests extends LdifReaderTestSupport {

	@Test
	void readRecordsUsingSetters() throws Exception {
		LdifReader reader = new LdifReader(ldifResource);
		reader.setName("ldif");
		reader.setStrict(true);
		reader.afterPropertiesSet();

		verify(reader, expectedDns());
	}

	@Test
	void missingResourceInStrictModeShouldFailOnOpen() throws Exception {
		LdifReader reader = new LdifReader(missingResource());
		reader.setName("ldif");
		reader.setStrict(true);
		reader.afterPropertiesSet();

		assertThrows(Exception.class, () -> reader.open(new ExecutionContext()));
	}

	@Test
	void skippedRecordsCallbackIsInvoked() throws Exception {
		StringBuilder callback = new StringBuilder();

		LdifReader reader = new LdifReader(ldifResource);
		reader.setName("ldif");
		reader.setRecordsToSkip(1);
		reader.setSkippedRecordsCallback(attributes -> callback.append(attributes.getName().toString()));
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
		reader.read();
		reader.close();

		assertEquals("cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com", callback.toString());
	}

}
