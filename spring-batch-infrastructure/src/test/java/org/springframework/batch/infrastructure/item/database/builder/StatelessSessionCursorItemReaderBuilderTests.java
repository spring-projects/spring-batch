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
package org.springframework.batch.infrastructure.item.database.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.StatelessSessionCursorItemReader;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Philippe Marschall
 */
@SpringJUnitConfig
class StatelessSessionCursorItemReaderBuilderTests {

	@Autowired
	private SessionFactory sessionFactory;

	@Test
	void testConfiguration() throws Exception {
		StatelessSessionCursorItemReader<Foo> reader = new StatelessSessionCursorItemReaderBuilder<>(Foo.class)
			.name("fooReader")
			.sessionFactory(this.sessionFactory)
			.currentItemCount(2)
			.maxItemCount(4)
			.queryString("select f from Foo f order by f.id")
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
		Map<String, Object> parameters = Map.of("value", 2);

		StatelessSessionCursorItemReader<Foo> reader = new StatelessSessionCursorItemReaderBuilder<>(Foo.class)
			.name("fooReader")
			.sessionFactory(this.sessionFactory)
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

		StatelessSessionCursorItemReader<Foo> reader = new StatelessSessionCursorItemReaderBuilder<>(Foo.class)
			.name("fooReader")
			.sessionFactory(this.sessionFactory)
			.namedQuery("allFoos")
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

		StatelessSessionCursorItemReader<Foo> reader = new StatelessSessionCursorItemReaderBuilder<>(Foo.class)
			.name("fooReader")
			.sessionFactory(this.sessionFactory)
			.nativeQuery("select * from T_FOOS")
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
		var builder = new StatelessSessionCursorItemReaderBuilder<>(Foo.class);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("An SessionFactory is required", exception.getMessage());

		builder = new StatelessSessionCursorItemReaderBuilder<>(Foo.class).sessionFactory(this.sessionFactory)
			.saveState(false);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("Query string is required when queryProvider is null", exception.getMessage());
	}

	@Configuration
	public static class TestDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true)
				.addScript("org/springframework/batch/infrastructure/item/database/init-foo-schema.sql")
				.build();
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
