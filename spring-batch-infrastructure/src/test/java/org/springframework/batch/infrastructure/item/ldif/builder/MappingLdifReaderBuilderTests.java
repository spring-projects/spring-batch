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
package org.springframework.batch.infrastructure.item.ldif.builder;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ldif.MappingLdifReader;
import org.springframework.batch.infrastructure.item.ldif.RecordCallbackHandler;
import org.springframework.batch.infrastructure.item.ldif.RecordMapper;
import org.springframework.batch.infrastructure.item.ldif.support.MappingLdifReaderTestSupport;
import org.springframework.ldap.core.LdapAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Banseok Kim
 */
class MappingLdifReaderBuilderTests extends MappingLdifReaderTestSupport {

	private String callbackDn;

	@Test
	void itemReader_basicRead_mapsToDnString() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(ldifResource)
			.recordMapper(new StringMapper())
			.build();

		verify(reader, expectedDns());
	}

	@Test
	void recordsToSkip_skipsFirstRecord() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(ldifResource)
			.recordsToSkip(1)
			.recordMapper(new StringMapper())
			.build();

		reader.open(new ExecutionContext());
		String item = reader.read();
		assertEquals("cn=Bjorn Jensen,ou=Accounting,dc=airius,dc=com", item);
		reader.close();
	}

	@Test
	void currentItemCount_startsFromThirdRecord() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(ldifResource)
			.currentItemCount(2)
			.recordMapper(new StringMapper())
			.build();

		reader.open(new ExecutionContext());
		String item = reader.read();
		assertEquals("cn=Gern Jensen,ou=Product Testing,dc=airius,dc=com", item);
		reader.close();
	}

	@Test
	void currentItemCountAtEndReturnsNull() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(ldifResource)
			.currentItemCount(3)
			.recordMapper(new StringMapper())
			.build();

		reader.open(new ExecutionContext());
		Assertions.assertNull(reader.read());
		reader.close();
	}

	@Test
	void skippedRecordsCallback_isInvoked() throws Exception {
		this.callbackDn = null;

		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(ldifResource)
			.recordsToSkip(1)
			.recordMapper(new StringMapper())
			.skippedRecordsCallback(new TestCallback())
			.build();

		reader.open(new ExecutionContext());
		reader.read();
		assertEquals("cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com", this.callbackDn);
		reader.close();
	}

	@Test
	void saveState_updatesExecutionContext() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("foo")
			.resource(ldifResource)
			.recordMapper(new StringMapper())
			.build();

		ExecutionContext ec = new ExecutionContext();
		reader.open(ec);
		reader.read();
		reader.update(ec);

		assertEquals(1, ec.getInt("foo.read.count"));
		reader.close();
	}

	@Test
	void strictTrue_throwsOnMissingResource() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(missingResource())
			.recordMapper(new StringMapper())
			.strict(true)
			.build();

		ItemStreamException ex = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
		assertEquals("Failed to initialize the reader", ex.getMessage());
	}

	@Test
	void strictFalse_doesNotThrowOnMissingResource() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().name("ldif")
			.resource(missingResource())
			.recordMapper(new StringMapper())
			.strict(false)
			.build();

		reader.open(new ExecutionContext());
		reader.close();
	}

	@Test
	void itemReaderWithNoRecordMapperShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new MappingLdifReaderBuilder<String>().name("ldif").resource(ldifResource).build());
	}

	@Test
	void itemReaderWithNoResourceShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new MappingLdifReaderBuilder<String>().name("ldif").recordMapper(new StringMapper()).build());
	}

	@Test
	void itemReaderWithNoNameAndDefaultSaveStateShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new MappingLdifReaderBuilder<String>().resource(ldifResource)
					.recordMapper(new StringMapper())
					.build());
	}

	@Test
	void itemReaderWithNoNameButSaveStateFalseShouldSucceed() throws Exception {
		MappingLdifReader<String> reader = new MappingLdifReaderBuilder<String>().resource(ldifResource)
			.saveState(false)
			.recordMapper(new StringMapper())
			.build();

		verify(reader, expectedDns());
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

	private class TestCallback implements RecordCallbackHandler {

		@Override
		public void handleRecord(LdapAttributes attributes) {
			callbackDn = attributes.getName().toString();
		}

	}

}
