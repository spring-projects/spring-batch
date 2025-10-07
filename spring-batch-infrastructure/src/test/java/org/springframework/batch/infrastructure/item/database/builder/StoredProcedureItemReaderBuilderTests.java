/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.builder;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.infrastructure.item.database.FooRowMapper;
import org.springframework.batch.infrastructure.item.database.StoredProcedureItemReader;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class StoredProcedureItemReaderBuilderTests {

	private final DataSource dataSource = Mockito.mock();

	@Test
	void testConfiguration() {
		ArgumentPreparedStatementSetter preparedStatementSetter = new ArgumentPreparedStatementSetter(null);

		SqlParameter[] parameters = new SqlParameter[0];

		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReaderBuilder<Foo>().name("foo_reader")
			.dataSource(this.dataSource)
			.procedureName("read_foos")
			.rowMapper(new FooRowMapper())
			.verifyCursorPosition(false)
			.refCursorPosition(3)
			.useSharedExtendedConnection(true)
			.preparedStatementSetter(preparedStatementSetter)
			.parameters(parameters)
			.function()
			.fetchSize(5)
			.driverSupportsAbsolute(true)
			.currentItemCount(6)
			.ignoreWarnings(false)
			.maxItemCount(7)
			.queryTimeout(8)
			.maxRows(9)
			.build();

		assertEquals(3, ReflectionTestUtils.getField(reader, "refCursorPosition"));
		assertEquals(preparedStatementSetter, ReflectionTestUtils.getField(reader, "preparedStatementSetter"));
		assertEquals(parameters, ReflectionTestUtils.getField(reader, "parameters"));
		assertEquals(5, ReflectionTestUtils.getField(reader, "fetchSize"));
		assertEquals(6, ReflectionTestUtils.getField(reader, "currentItemCount"));
		assertEquals(7, ReflectionTestUtils.getField(reader, "maxItemCount"));
		assertEquals(8, ReflectionTestUtils.getField(reader, "queryTimeout"));
		assertEquals(9, ReflectionTestUtils.getField(reader, "maxRows"));
		assertTrue((Boolean) ReflectionTestUtils.getField(reader, "useSharedExtendedConnection"));
		assertTrue((Boolean) ReflectionTestUtils.getField(reader, "function"));
		assertTrue((Boolean) ReflectionTestUtils.getField(reader, "driverSupportsAbsolute"));
		assertFalse((Boolean) ReflectionTestUtils.getField(reader, "ignoreWarnings"));
	}

	@Test
	void testValidation() {
		var builder = new StoredProcedureItemReaderBuilder<Foo>();
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A name is required when saveSate is set to true", exception.getMessage());

		builder = new StoredProcedureItemReaderBuilder<Foo>().saveState(false);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("The name of the stored procedure must be provided", exception.getMessage());

		builder = new StoredProcedureItemReaderBuilder<Foo>().saveState(false).procedureName("read_foos");
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A datasource is required", exception.getMessage());

		builder = new StoredProcedureItemReaderBuilder<Foo>().saveState(false)
			.procedureName("read_foos")
			.dataSource(this.dataSource);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A rowmapper is required", exception.getMessage());
	}

}
