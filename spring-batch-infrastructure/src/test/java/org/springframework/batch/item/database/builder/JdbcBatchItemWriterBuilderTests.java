/*
 * Copyright 2016-2025 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class JdbcBatchItemWriterBuilderTests {

	private DataSource dataSource;

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class);
		this.dataSource = context.getBean("dataSource", DataSource.class);
	}

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testBasicMap() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.columnMapped()
			.dataSource(this.dataSource)
			.sql("INSERT INTO FOO (first, second, third) VALUES (:first, :second, :third)")
			.build();

		writer.afterPropertiesSet();

		Chunk<Map<String, Object>> chunk = buildMapItems();
		writer.write(chunk);

		verifyWrite();
	}

	@Test
	void testCustomJdbcTemplate() throws Exception {
		NamedParameterJdbcOperations template = new NamedParameterJdbcTemplate(this.dataSource);

		JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.columnMapped()
			.namedParametersJdbcTemplate(template)
			.sql("INSERT INTO FOO (first, second, third) VALUES (:first, :second, :third)")
			.build();

		writer.afterPropertiesSet();

		Chunk<Map<String, Object>> items = buildMapItems();
		writer.write(items);

		verifyWrite();

		Object usedTemplate = ReflectionTestUtils.getField(writer, "namedParameterJdbcTemplate");
		assertSame(template, usedTemplate);
	}

	@Test
	void testBasicPojo() throws Exception {
		JdbcBatchItemWriter<Foo> writer = new JdbcBatchItemWriterBuilder<Foo>().beanMapped()
			.dataSource(this.dataSource)
			.sql("INSERT INTO FOO (first, second, third) VALUES (:first, :second, :third)")
			.build();

		writer.afterPropertiesSet();

		Chunk<Foo> items = new Chunk<>();

		items.add(new Foo(1, "two", "three"));
		items.add(new Foo(4, "five", "six"));
		items.add(new Foo(7, "eight", "nine"));

		writer.write(items);

		verifyWrite();
	}

	@Test
	void testAssertUpdates() {
		JdbcBatchItemWriter<Foo> writer = new JdbcBatchItemWriterBuilder<Foo>().beanMapped()
			.dataSource(this.dataSource)
			.sql("UPDATE FOO SET second = :second, third = :third WHERE first = :first")
			.assertUpdates(true)
			.build();

		writer.afterPropertiesSet();

		Chunk<Foo> items = new Chunk<>();

		items.add(new Foo(1, "two", "three"));

		assertThrows(EmptyResultDataAccessException.class, () -> writer.write(items));
	}

	@Test
	void testCustomPreparedStatementSetter() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.itemPreparedStatementSetter((item, ps) -> {
				ps.setInt(0, (int) item.get("first"));
				ps.setString(1, (String) item.get("second"));
				ps.setString(2, (String) item.get("third"));
			})
			.dataSource(this.dataSource)
			.sql("INSERT INTO FOO (first, second, third) VALUES (:first, :second, :third)")
			.build();

		writer.afterPropertiesSet();

		Chunk<Map<String, Object>> items = buildMapItems();
		writer.write(items);

		verifyWrite();
	}

	@Test
	void testCustomPSqlParameterSourceProvider() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.itemSqlParameterSourceProvider(MapSqlParameterSource::new)
			.dataSource(this.dataSource)
			.sql("INSERT INTO FOO (first, second, third) VALUES (:first, :second, :third)")
			.build();

		writer.afterPropertiesSet();

		Chunk<Map<String, Object>> items = buildMapItems();
		writer.write(items);

		verifyWrite();
	}

	@Test
	void testBuildAssertions() {
		var builder = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.itemSqlParameterSourceProvider(MapSqlParameterSource::new);
		Exception exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("Either a DataSource or a NamedParameterJdbcTemplate is required", exception.getMessage());

		builder = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
			.itemSqlParameterSourceProvider(MapSqlParameterSource::new)
			.dataSource(this.dataSource);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A SQL statement is required", exception.getMessage());

		builder = new JdbcBatchItemWriterBuilder<Map<String, Object>>().dataSource(this.dataSource)
			.sql("INSERT INTO FOO VALUES (?, ?, ?)")
			.columnMapped()
			.beanMapped();
		exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("Either an item can be mapped via db column or via bean spec, can't be both",
				exception.getMessage());
	}

	private void verifyWrite() {
		verifyRow(1, "two", "three");
		verifyRow(4, "five", "six");
		verifyRow(7, "eight", "nine");
	}

	private Chunk<Map<String, Object>> buildMapItems() {
		Chunk<Map<String, Object>> items = new Chunk<>();

		Map<String, Object> item = new HashMap<>(3);
		item.put("first", 1);
		item.put("second", "two");
		item.put("third", "three");
		items.add(item);

		item = new HashMap<>(3);
		item.put("first", 4);
		item.put("second", "five");
		item.put("third", "six");
		items.add(item);

		item = new HashMap<>(3);
		item.put("first", 7);
		item.put("second", "eight");
		item.put("third", "nine");
		items.add(item);
		return items;
	}

	private void verifyRow(int i, String i1, String nine) {
		JdbcOperations template = new JdbcTemplate(this.dataSource);

		String sql = "select count(*) from foo where first = ? and second = ? and third = ?";
		assertEquals(1, (int) template.queryForObject(sql, Integer.class, i, i1, nine));
	}

	public static class Foo {

		private int first;

		private String second;

		private String third;

		public Foo(int first, String second, String third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

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

	@Configuration
	public static class TestDataSourceConfiguration {

		private static final String CREATE_SQL = """
				CREATE TABLE FOO  (
				ID BIGINT IDENTITY NOT NULL PRIMARY KEY ,
				FIRST BIGINT ,
				SECOND VARCHAR(5) NOT NULL,
				THIRD VARCHAR(5) NOT NULL) ;""";

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		public DataSourceInitializer initializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);

			Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create));

			return dataSourceInitializer;
		}

	}

}
