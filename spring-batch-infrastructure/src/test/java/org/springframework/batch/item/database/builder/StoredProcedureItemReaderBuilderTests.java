/*
 * Copyright 2017-2019 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.jdbc.datasource.DataSourceInitializer;
import test.jdbc.datasource.DerbyDataSourceFactoryBean;
import test.jdbc.datasource.DerbyShutdownBean;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.FooRowMapper;
import org.springframework.batch.item.database.StoredProcedureItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class StoredProcedureItemReaderBuilderTests {

	private DataSource dataSource;

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class);
		this.dataSource = (DataSource) this.context.getBean("dataSource");
	}

	@After
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void testSunnyScenario() throws Exception {
		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReaderBuilder<Foo>()
				.name("foo_reader")
				.dataSource(this.dataSource)
				.procedureName("read_foos")
				.rowMapper(new FooRowMapper())
				.verifyCursorPosition(false)
				.build();

		reader.open(new ExecutionContext());

		Foo item1 = reader.read();
		assertEquals(1, item1.getId());
		assertEquals("bar1", item1.getName());
		assertEquals(1, item1.getValue());

		reader.close();
	}

	@Test
	public void testConfiguration() {
		ArgumentPreparedStatementSetter preparedStatementSetter = new ArgumentPreparedStatementSetter(null);

		SqlParameter[] parameters = new SqlParameter[0];

		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReaderBuilder<Foo>()
				.name("foo_reader")
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
	public void testNoSaveState() throws Exception {
		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReaderBuilder<Foo>()
				.dataSource(this.dataSource)
				.procedureName("read_foos")
				.rowMapper(new FooRowMapper())
				.verifyCursorPosition(false)
				.saveState(false)
				.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		reader.read();
		reader.read();

		reader.update(executionContext);

		assertEquals(0, executionContext.size());

		reader.close();
	}

	@Test
	public void testValidation() {
		try {
			new StoredProcedureItemReaderBuilder<Foo>()
					.build();

			fail("Exception was not thrown for missing the name");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("A name is required when saveSate is set to true",
					iae.getMessage());
		}

		try {
			new StoredProcedureItemReaderBuilder<Foo>()
					.saveState(false)
					.build();

			fail("Exception was not thrown for missing the stored procedure name");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("The name of the stored procedure must be provided",
					iae.getMessage());
		}

		try {
			new StoredProcedureItemReaderBuilder<Foo>()
					.saveState(false)
					.procedureName("read_foos")
					.build();

			fail("Exception was not thrown for missing the DataSource");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("A datasource is required",
					iae.getMessage());
		}

		try {
			new StoredProcedureItemReaderBuilder<Foo>()
					.saveState(false)
					.procedureName("read_foos")
					.dataSource(this.dataSource)
					.build();

			fail("Exception was not thrown for missing the RowMapper");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("A rowmapper is required",
					iae.getMessage());
		}
	}

	@Configuration
	public static class TestDataSourceConfiguration {

		@Bean
		public DerbyDataSourceFactoryBean dataSource() {
			DerbyDataSourceFactoryBean derbyDataSourceFactoryBean = new DerbyDataSourceFactoryBean();

			derbyDataSourceFactoryBean.setDataDirectory("build/derby-home");

			return derbyDataSourceFactoryBean;
		}

		@Bean
		public DerbyShutdownBean dbShutdown(DataSource dataSource) {
			DerbyShutdownBean shutdownBean = new DerbyShutdownBean();

			shutdownBean.setDataSource(dataSource);

			return shutdownBean;
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();

			transactionManager.setDataSource(dataSource);

			return transactionManager;
		}

		@Bean
		public DataSourceInitializer initializer(DataSource dataSource) {
			DataSourceInitializer initializer = new DataSourceInitializer();

			initializer.setDataSource(dataSource);
			initializer.setInitScripts(new ClassPathResource[]{
					new ClassPathResource("org/springframework/batch/item/database/init-foo-schema-derby.sql")
			});
			initializer.setDestroyScripts(new ClassPathResource[]{
					new ClassPathResource("org/springframework/batch/item/database/drop-foo-schema-derby.sql")
			});

			return initializer;
		}
	}
}
