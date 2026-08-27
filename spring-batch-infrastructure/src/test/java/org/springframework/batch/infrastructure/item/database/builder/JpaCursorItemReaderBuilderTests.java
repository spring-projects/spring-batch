/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.orm.JpaNamedQueryProvider;
import org.springframework.batch.infrastructure.item.database.orm.JpaNativeQueryProvider;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 * @author Andrey Litvitski
 */
class JpaCursorItemReaderBuilderTests {

	private EntityManagerFactory entityManagerFactory;

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext(
				JpaCursorItemReaderBuilderTests.TestDataSourceConfiguration.class);
		this.entityManagerFactory = context.getBean("entityManagerFactory", EntityManagerFactory.class);
	}

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testConfiguration() throws Exception {
		JpaCursorItemReader<Foo> reader = new JpaCursorItemReaderBuilder<Foo>().name("fooReader")
			.entityManagerFactory(this.entityManagerFactory)
			.currentItemCount(2)
			.maxItemCount(4)
			.queryString("select f from Foo f ")
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);
		Foo item1 = reader.read();
		Foo item2 = reader.read();
		assertNull(reader.read());
		reader.update(executionContext);
		reader.close();

		assertEquals(3, item1.getId());
		assertEquals("bar3", item1.getName());
		assertEquals(3, item1.getValue());
		assertEquals(4, item2.getId());
		assertEquals("bar4", item2.getName());
		assertEquals(4, item2.getValue());

		assertEquals(2, executionContext.size());
	}

	@Test
	void testConfigurationNoSaveState() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("value", 2);

		JpaCursorItemReader<Foo> reader = new JpaCursorItemReaderBuilder<Foo>().name("fooReader")
			.entityManagerFactory(this.entityManagerFactory)
			.queryString("select f from Foo f where f.id > :value")
			.parameterValues(parameters)
			.saveState(false)
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);

		int i = 0;
		while (reader.read() != null) {
			i++;
		}

		reader.update(executionContext);
		reader.close();

		assertEquals(3, i);
		assertEquals(0, executionContext.size());
	}

	@Test
	void testConfigurationNamedQueryProvider() throws Exception {
		JpaNamedQueryProvider<Foo> namedQueryProvider = new JpaNamedQueryProvider<>();
		namedQueryProvider.setNamedQuery("allFoos");
		namedQueryProvider.setEntityClass(Foo.class);

		JpaCursorItemReader<Foo> reader = new JpaCursorItemReaderBuilder<Foo>().name("fooReader")
			.entityManagerFactory(this.entityManagerFactory)
			.queryProvider(namedQueryProvider)
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		Foo foo;
		List<Foo> foos = new ArrayList<>();

		while ((foo = reader.read()) != null) {
			foos.add(foo);
		}

		reader.update(executionContext);
		reader.close();

		int id = 0;
		for (Foo testFoo : foos) {
			assertEquals(++id, testFoo.getId());
		}
	}

	@Test
	void testConfigurationNativeQueryProvider() throws Exception {

		JpaNativeQueryProvider<Foo> provider = new JpaNativeQueryProvider<>();
		provider.setEntityClass(Foo.class);
		provider.setSqlQuery("select * from T_FOOS");
		provider.afterPropertiesSet();

		JpaCursorItemReader<Foo> reader = new JpaCursorItemReaderBuilder<Foo>().name("fooReader")
			.entityManagerFactory(this.entityManagerFactory)
			.queryProvider(provider)
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);

		int i = 0;
		while (reader.read() != null) {
			i++;
		}

		reader.update(executionContext);
		reader.close();

		assertEquals(5, i);
	}

	@Test
	void testValidation() {
		var builder = new JpaCursorItemReaderBuilder<Foo>();
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("An EntityManagerFactory is required", exception.getMessage());

		builder = new JpaCursorItemReaderBuilder<Foo>().entityManagerFactory(this.entityManagerFactory)
			.saveState(false);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("Query string is required when queryProvider is null", exception.getMessage());
	}

	@Configuration
	public static class TestDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		public DataSourceInitializer initializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);

			Resource create = new ClassPathResource(
					"org/springframework/batch/infrastructure/item/database/init-foo-schema.sql");
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create));

			return dataSourceInitializer;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();

			entityManagerFactoryBean.setDataSource(dataSource());
			entityManagerFactoryBean.setPersistenceUnitName("foo");
			entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

			return entityManagerFactoryBean;
		}

	}

}
