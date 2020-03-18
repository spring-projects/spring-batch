/*
 * Copyright 2017-2018 the original author or authors.
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

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.HibernateItemReaderHelper;
import org.springframework.batch.item.database.HibernatePagingItemReader;
import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class HibernatePagingItemReaderBuilderTests {

	private SessionFactory sessionFactory;

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext(HibernatePagingItemReaderBuilderTests.TestDataSourceConfiguration.class);
		this.sessionFactory = (SessionFactory) context.getBean("sessionFactory");
	}

	@After
	public void tearDown() {
		if(this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConfiguration() throws Exception {
		HibernatePagingItemReader<Foo> reader = new HibernatePagingItemReaderBuilder<Foo>()
				.name("fooReader")
				.sessionFactory(this.sessionFactory)
				.fetchSize(2)
				.currentItemCount(2)
				.maxItemCount(4)
				.pageSize(5)
				.queryName("allFoos")
				.useStatelessSession(false)
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
		assertEquals(5, ReflectionTestUtils.getField(reader, "pageSize"));

		HibernateItemReaderHelper<Foo> helper = (HibernateItemReaderHelper<Foo>) ReflectionTestUtils.getField(reader, "helper");
		assertEquals(false, ReflectionTestUtils.getField(helper, "useStatelessSession"));
	}

	@Test
	public void testConfigurationNoSaveState() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("value", 2);

		HibernatePagingItemReader<Foo> reader = new HibernatePagingItemReaderBuilder<Foo>()
				.name("fooReader")
				.sessionFactory(this.sessionFactory)
				.queryString("from Foo foo where foo.id > :value")
				.parameterValues(parameters)
				.saveState(false)
				.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);

		int i = 0;
		while(reader.read() != null) {
			i++;
		}

		reader.update(executionContext);
		reader.close();

		assertEquals(3, i);
		assertEquals(0, executionContext.size());
	}

	@Test
	public void testConfigurationQueryProvider() throws Exception {

		HibernateNativeQueryProvider<Foo> provider = new HibernateNativeQueryProvider<>();
		provider.setEntityClass(Foo.class);
		provider.setSqlQuery("select * from T_FOOS");
		provider.afterPropertiesSet();

		HibernatePagingItemReader<Foo> reader = new HibernatePagingItemReaderBuilder<Foo>()
				.name("fooReader")
				.sessionFactory(this.sessionFactory)
				.queryProvider(provider)
				.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);

		int i = 0;
		while(reader.read() != null) {
			i++;
		}

		reader.update(executionContext);
		reader.close();

		assertEquals(5, i);
	}

	@Test
	public void testValidation() {
		try {
			new HibernatePagingItemReaderBuilder<Foo>()
					.sessionFactory(this.sessionFactory)
					.fetchSize(-2)
					.build();
			fail("fetch size must be >= 0");
		}
		catch (IllegalStateException ise) {
			assertEquals("fetchSize must not be negative", ise.getMessage());
		}

		try {
			new HibernatePagingItemReaderBuilder<Foo>().build();
			fail("A SessionFactory must be provided");
		}
		catch (IllegalArgumentException ise) {
			assertEquals("A SessionFactory must be provided", ise.getMessage());
		}

		try {
			new HibernatePagingItemReaderBuilder<Foo>()
					.sessionFactory(this.sessionFactory)
					.saveState(true)
					.build();
			fail("name is required when saveState is set to true");
		}
		catch (IllegalArgumentException ise) {
			assertEquals("A name is required when saveState is set to true", ise.getMessage());
		}

		try {
			new HibernatePagingItemReaderBuilder<Foo>()
					.sessionFactory(this.sessionFactory)
					.saveState(false)
					.build();
			fail("queryString or queryName must be set");
		}
		catch (IllegalStateException ise) {
			assertEquals("queryString or queryName must be set", ise.getMessage());
		}

	}

	@Configuration
	public static class TestDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseFactory().getDatabase();
		}

		@Bean
		public DataSourceInitializer initializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);

			Resource create = new ClassPathResource("org/springframework/batch/item/database/init-foo-schema-hsqldb.sql");
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create));

			return dataSourceInitializer;
		}

		@Bean
		public SessionFactory sessionFactory() throws Exception {
			LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
			factoryBean.setDataSource(dataSource());
			factoryBean.setMappingLocations(new ClassPathResource("/org/springframework/batch/item/database/Foo.hbm.xml", getClass()));
			factoryBean.afterPropertiesSet();

			return factoryBean.getObject();

		}
	}
}
