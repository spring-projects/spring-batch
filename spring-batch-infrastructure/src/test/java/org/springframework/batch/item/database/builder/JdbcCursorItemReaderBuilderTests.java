/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import java.sql.Types;
import java.util.Arrays;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Michael Minella
 * @author Drummond Dawson
 * @author Ankur Trapasiya
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 * @author Juyoung Kim
 */
class JdbcCursorItemReaderBuilderTests {

	private DataSource dataSource;

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class);
		this.dataSource = (DataSource) context.getBean("dataSource");
	}

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testSimpleScenario() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 1, "2", "3");
		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testMaxRows() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.maxRows(2)
			.saveState(false)
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 1, "2", "3");
		validateFoo(reader.read(), 4, "5", "6");
		assertNull(reader.read());

		reader.close();
		assertEquals(0, executionContext.size());
	}

	@Test
	void testQueryArgumentsList() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO WHERE FIRST > ? ORDER BY FIRST")
			.queryArguments(Arrays.asList(3))
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testQueryArgumentsArray() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO WHERE FIRST > ? ORDER BY FIRST")
			.queryArguments(3)
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testQueryArgumentsTypedArray() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO WHERE FIRST > ? ORDER BY FIRST")
			.queryArguments(new Integer[] { 3 }, new int[] { Types.BIGINT })
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testPreparedStatementSetter() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO WHERE FIRST > ? ORDER BY FIRST")
			.preparedStatementSetter(ps -> ps.setInt(1, 3))
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testMaxItemCount() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.maxItemCount(2)
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 1, "2", "3");
		validateFoo(reader.read(), 4, "5", "6");

		assertNull(reader.read());
	}

	@Test
	void testCurrentItemCount() throws Exception {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.currentItemCount(1)
			.rowMapper((rs, rowNum) -> {
				Foo foo = new Foo();

				foo.setFirst(rs.getInt("FIRST"));
				foo.setSecond(rs.getString("SECOND"));
				foo.setThird(rs.getString("THIRD"));

				return foo;
			})
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		validateFoo(reader.read(), 4, "5", "6");
		validateFoo(reader.read(), 7, "8", "9");

		assertNull(reader.read());
	}

	@Test
	void testOtherProperties() {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.fetchSize(1)
			.queryTimeout(2)
			.ignoreWarnings(true)
			.driverSupportsAbsolute(true)
			.useSharedExtendedConnection(true)
			.connectionAutoCommit(true)
			.beanRowMapper(Foo.class)
			.build();

		assertEquals(1, ReflectionTestUtils.getField(reader, "fetchSize"));
		assertEquals(2, ReflectionTestUtils.getField(reader, "queryTimeout"));
		assertTrue((boolean) ReflectionTestUtils.getField(reader, "ignoreWarnings"));
		assertTrue((boolean) ReflectionTestUtils.getField(reader, "driverSupportsAbsolute"));
		assertTrue((boolean) ReflectionTestUtils.getField(reader, "connectionAutoCommit"));
	}

	@Test
	void testVerifyCursorPositionDefaultToTrue() {
		JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>().dataSource(this.dataSource)
			.name("fooReader")
			.sql("SELECT * FROM FOO ORDER BY FIRST")
			.beanRowMapper(Foo.class)
			.build();
		assertTrue((boolean) ReflectionTestUtils.getField(reader, "verifyCursorPosition"));
	}

	@Test
	void testValidation() {
		var builder = new JdbcCursorItemReaderBuilder<Foo>().saveState(true);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A name is required when saveState is set to true", exception.getMessage());

		builder = new JdbcCursorItemReaderBuilder<Foo>().saveState(false);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A query is required", exception.getMessage());

		builder = new JdbcCursorItemReaderBuilder<Foo>().saveState(false).sql("select 1");
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A datasource is required", exception.getMessage());

		builder = new JdbcCursorItemReaderBuilder<Foo>().saveState(false).sql("select 1").dataSource(this.dataSource);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A rowmapper is required", exception.getMessage());
	}

	private void validateFoo(Foo item, int first, String second, String third) {
		assertEquals(first, item.getFirst());
		assertEquals(second, item.getSecond());
		assertEquals(third, item.getThird());
	}

	@Test
	void testDataRowMapper() throws Exception {
		JdbcCursorItemReader<Bar> reader = new JdbcCursorItemReaderBuilder<Bar>()
				.name("barReader")
				.dataSource(this.dataSource)
				.currentItemCount(1)
				.maxItemCount(2)
				.sql("SELECT ID, FIRST, SECOND, THIRD FROM BAR ORDER BY ID DESC")
				.dataRowMapper(Bar.class)
				.build();

		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
		Bar item1 = reader.read();
		assertNull(reader.read());

		assertEquals(3, item1.id());
		assertEquals(10, item1.first());
		assertEquals("11", item1.second());
		assertEquals("12", item1.third());
	}

	public static class Foo {

		private int first;

		private String second;

		private String third;

		public int getFirst() {
			return first;
		}

		public void setFirst(int first) {
			this.first = first;
		}

		public String getSecond() {
			return second;
		}

		public void setSecond(String second) {
			this.second = second;
		}

		public String getThird() {
			return third;
		}

		public void setThird(String third) {
			this.third = third;
		}

	}

	public record Bar(int id, int first, String second, String third) {}

	@Configuration
	public static class TestDataSourceConfiguration {

		private static final String CREATE_SQL = """
				CREATE TABLE FOO  (
				ID BIGINT IDENTITY NOT NULL PRIMARY KEY ,
				FIRST BIGINT ,
				SECOND VARCHAR(5) NOT NULL,
				THIRD VARCHAR(5) NOT NULL);
				
				CREATE TABLE BAR  (
				ID BIGINT IDENTITY NOT NULL PRIMARY KEY ,
				FIRST BIGINT ,
				SECOND VARCHAR(5) NOT NULL,
				THIRD VARCHAR(5) NOT NULL) ;""";

		private static final String INSERT_SQL = """
				INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (1, '2', '3');
				INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (4, '5', '6');
				INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (7, '8', '9');
				
				INSERT INTO BAR (FIRST, SECOND, THIRD) VALUES (1, '2', '3');
				INSERT INTO BAR (FIRST, SECOND, THIRD) VALUES (4, '5', '6');
				INSERT INTO BAR (FIRST, SECOND, THIRD) VALUES (7, '8', '9');
				INSERT INTO BAR (FIRST, SECOND, THIRD) VALUES (10, '11', '12');
				INSERT INTO BAR (FIRST, SECOND, THIRD) VALUES (13, '14', '15');""";

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		public DataSourceInitializer initializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);

			Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
			Resource insert = new ByteArrayResource(INSERT_SQL.getBytes());
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create, insert));

			return dataSourceInitializer;
		}

	}

}
