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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ldif.LdifReader;
import org.springframework.batch.infrastructure.item.ldif.RecordCallbackHandler;
import org.springframework.batch.infrastructure.item.ldif.support.LdifReaderTestSupport;
import org.springframework.ldap.core.LdapAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Banseok Kim
 */
class LdifReaderBuilderTests extends LdifReaderTestSupport {

	private String callbackDn;

	@Test
	void itemReaderBasicRead() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(ldifResource).build();

		verify(reader, expectedDns());
	}

	@Test
	void recordsToSkipSkipsFirstRecord() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(ldifResource).recordsToSkip(1).build();

		reader.open(new ExecutionContext());
		LdapAttributes item = reader.read();
		assertEquals("cn=Bjorn Jensen,ou=Accounting,dc=airius,dc=com", item.getName().toString());
		reader.close();
	}

	@Test
	void currentItemCountStartsFromThirdRecord() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(ldifResource).currentItemCount(2).build();

		reader.open(new ExecutionContext());
		LdapAttributes item = reader.read();
		assertEquals("cn=Gern Jensen,ou=Product Testing,dc=airius,dc=com", item.getName().toString());
		reader.close();
	}

	@Test
	void currentItemCountAtEndReturnsNull() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(ldifResource).currentItemCount(3).build();

		reader.open(new ExecutionContext());
		Assertions.assertNull(reader.read());
		reader.close();
	}

	@Test
	void maxItemCountLimitsReads() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(ldifResource).maxItemCount(1).build();

		reader.open(new ExecutionContext());
		LdapAttributes first = reader.read();
		LdapAttributes second = reader.read();
		assertEquals("cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com", first.getName().toString());
		assertNull(second);
		reader.close();
	}

	@Test
	void skippedRecordsCallbackIsInvoked() throws Exception {
		this.callbackDn = null;

		LdifReader reader = new LdifReaderBuilder().name("ldif")
			.resource(ldifResource)
			.recordsToSkip(1)
			.skippedRecordsCallback(new TestCallback())
			.build();

		reader.open(new ExecutionContext());
		reader.read();
		assertEquals("cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com", this.callbackDn);
		reader.close();
	}

	@Test
	void saveStateUpdatesExecutionContext() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("foo").resource(ldifResource).build();

		ExecutionContext ec = new ExecutionContext();
		reader.open(ec);
		reader.read();
		reader.update(ec);

		assertEquals(1, ec.getInt("foo.read.count"));
		reader.close();
	}

	@Test
	void strictTrueThrowsOnMissingResource() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(missingResource()).strict(true).build();

		ItemStreamException ex = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
		assertEquals("Failed to initialize the reader", ex.getMessage());
	}

	@Test
	void strictFalseDoesNotThrowOnMissingResource() throws Exception {
		LdifReader reader = new LdifReaderBuilder().name("ldif").resource(missingResource()).strict(false).build();

		reader.open(new ExecutionContext());
		reader.close();
	}

	@Test
	void itemReaderWithNoResourceShouldFail() {
		assertThrows(IllegalArgumentException.class, () -> new LdifReaderBuilder().name("ldif").build());
	}

	@Test
	void itemReaderWithNoNameAndDefaultSaveStateShouldFail() {
		assertThrows(IllegalArgumentException.class, () -> new LdifReaderBuilder().resource(ldifResource).build());
	}

	@Test
	void itemReaderWithNoNameButSaveStateFalseShouldSucceed() throws Exception {
		LdifReader reader = new LdifReaderBuilder().resource(ldifResource).saveState(false).build();

		verify(reader, expectedDns());
	}

	private class TestCallback implements RecordCallbackHandler {

		@Override
		public void handleRecord(LdapAttributes attributes) {
			callbackDn = attributes.getName().toString();
		}

	}

}
