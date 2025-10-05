/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.batch.core.test.ldif.builder;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ldif.MappingLdifReader;
import org.springframework.batch.infrastructure.item.ldif.RecordCallbackHandler;
import org.springframework.batch.infrastructure.item.ldif.RecordMapper;
import org.springframework.batch.infrastructure.item.ldif.builder.MappingLdifReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapAttributes;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
class MappingLdifReaderBuilderTests {

	@Autowired
	private ApplicationContext context;

	private MappingLdifReader<LdapAttributes> mappingLdifReader;

	private String callbackAttributeName;

	@AfterEach
	void tearDown() {
		this.callbackAttributeName = null;
		if (this.mappingLdifReader != null) {
			this.mappingLdifReader.close();
		}
	}

	@Test
	void testSkipRecord() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().recordsToSkip(1)
			.recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		LdapAttributes ldapAttributes = firstRead();
		assertEquals("cn=Bjorn Jensen, ou=Accounting, dc=airius, dc=com", ldapAttributes.getName().toString(),
				"The attribute name for the second record did not match expected result");
	}

	@Test
	void testBasicRead() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		LdapAttributes ldapAttributes = firstRead();
		assertEquals("cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com",
				ldapAttributes.getName().toString(),
				"The attribute name for the first record did not match expected result");
	}

	@Test
	void testCurrentItemCount() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().currentItemCount(3)
			.recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		LdapAttributes ldapAttributes = firstRead();
		assertEquals("cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com", ldapAttributes.getName().toString(),
				"The attribute name for the third record did not match expected result");
	}

	@Test
	void testMaxItemCount() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().maxItemCount(1)
			.recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		LdapAttributes ldapAttributes = firstRead();
		assertEquals("cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com",
				ldapAttributes.getName().toString(),
				"The attribute name for the first record did not match expected result");
		ldapAttributes = this.mappingLdifReader.read();
		assertNull(ldapAttributes, "The second read should have returned null");
	}

	@Test
	void testSkipRecordCallback() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().recordsToSkip(1)
			.recordMapper(new TestMapper())
			.skippedRecordsCallback(new TestCallBackHandler())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		firstRead();
		assertEquals("cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com", this.callbackAttributeName,
				"The attribute name from the callback handler did not match the  expected result");
	}

	@Test
	void testSaveState() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.name("foo")
			.build();
		ExecutionContext executionContext = new ExecutionContext();
		firstRead(executionContext);
		this.mappingLdifReader.update(executionContext);
		assertEquals(1, executionContext.getInt("foo.read.count"), "foo.read.count did not have the expected result");
	}

	@Test
	void testSaveStateDisabled() throws Exception {
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().saveState(false)
			.recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/test.ldif"))
			.build();
		ExecutionContext executionContext = new ExecutionContext();
		firstRead(executionContext);
		this.mappingLdifReader.update(executionContext);
		assertEquals(0, executionContext.size(), "ExecutionContext should have been empty");
	}

	@Test
	void testStrict() throws Exception {
		// Test that strict when enabled will throw an exception.
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/teadsfst.ldif"))
			.name("foo")
			.build();
		Exception exception = assertThrows(ItemStreamException.class,
				() -> this.mappingLdifReader.open(new ExecutionContext()));
		assertEquals("Failed to initialize the reader", exception.getMessage(),
				"IllegalStateException message did not match the expected result.");
		// Test that strict when disabled will still allow the ldap resource to be opened.
		this.mappingLdifReader = new MappingLdifReaderBuilder<LdapAttributes>().strict(false)
			.name("foo")
			.recordMapper(new TestMapper())
			.resource(context.getResource("classpath:/teadsfst.ldif"))
			.build();
		this.mappingLdifReader.open(new ExecutionContext());
	}

	@Test
	void testNullRecordMapper() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new MappingLdifReaderBuilder<LdapAttributes>()
					.resource(context.getResource("classpath:/teadsfst.ldif"))
					.build());
		assertEquals("RecordMapper is required.", exception.getMessage(),
				"IllegalArgumentException message did not match the expected result.");
	}

	private LdapAttributes firstRead() throws Exception {
		return firstRead(new ExecutionContext());
	}

	private LdapAttributes firstRead(ExecutionContext executionContext) throws Exception {
		this.mappingLdifReader.open(executionContext);
		return this.mappingLdifReader.read();
	}

	@Configuration
	public static class LdifConfiguration {

	}

	public class TestCallBackHandler implements RecordCallbackHandler {

		@Override
		public void handleRecord(LdapAttributes attributes) {
			callbackAttributeName = attributes.getName().toString();
		}

	}

	public static class TestMapper implements RecordMapper<LdapAttributes> {

		@Override
		public @Nullable LdapAttributes mapRecord(LdapAttributes attributes) {
			return attributes;
		}

	}

}
