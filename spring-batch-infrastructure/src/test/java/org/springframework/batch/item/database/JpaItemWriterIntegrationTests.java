/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.batch.item.database;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.sample.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaItemWriterIntegrationTests.JpaConfiguration.class)
@Transactional
@DirtiesContext
public class JpaItemWriterIntegrationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void init() {
		this.jdbcTemplate.update("create table person (id int not null primary key, name varchar(32))");
	}

	@After
	public void destroy() {
		JdbcTestUtils.dropTables(this.jdbcTemplate, "person");
	}

	@Test
	public void testMerge() throws Exception {
		// given
		JpaItemWriter<Person> writer = new JpaItemWriter<>();
		writer.setEntityManagerFactory(this.entityManagerFactory);
		writer.afterPropertiesSet();
		List<Person> items = Arrays.asList(
				new Person(1, "foo"),
				new Person(2, "bar"));

		// when
		writer.write(items);

		// then
		assertEquals(2, JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "person"));
	}

	@Test
	public void testPersist() throws Exception {
		// given
		JpaItemWriter<Person> writer = new JpaItemWriter<>();
		writer.setEntityManagerFactory(this.entityManagerFactory);
		writer.setUsePersist(true);
		writer.afterPropertiesSet();
		List<Person> items = Arrays.asList(
				new Person(1, "foo"),
				new Person(2, "bar"));

		// when
		writer.write(items);

		// then
		assertEquals(2, JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "person"));
	}

	@Configuration
	public static class JpaConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.HSQL)
					.build();
		}

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public PersistenceUnitManager persistenceUnitManager() {
			DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
			persistenceUnitManager.setDefaultDataSource(dataSource());
			persistenceUnitManager.setPackagesToScan("org.springframework.batch.item.sample");
			persistenceUnitManager.afterPropertiesSet();
			return persistenceUnitManager;
		}

		@Bean
		public EntityManagerFactory entityManagerFactory() {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource());
			factoryBean.setPersistenceUnitManager(persistenceUnitManager());
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factoryBean.afterPropertiesSet();
			return factoryBean.getObject();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new JpaTransactionManager(entityManagerFactory());
		}
	}

}
