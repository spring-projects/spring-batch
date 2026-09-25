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
package org.springframework.batch.infrastructure.item.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.sample.Person;
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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManagerFactory;

@Rollback
@SpringJUnitConfig
@Transactional
class StatelessSessionItemWriterIntegrationTests {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void testInsertMultiple() throws Exception {
		// given
		StatelessSessionItemWriter<Person> writer = new StatelessSessionItemWriter<>(this.sessionFactory);
		Chunk<Person> items = Chunk.of(new Person(1, "foo"), new Person(2, "bar"));

		// when
		writer.write(items);

		// then
		assertEquals(2, JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "person"));
	}

	@Configuration
	public static class HibernateConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/infrastructure/item/database/init-person-schema.sql")
				.generateUniqueName(true)
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
			persistenceUnitManager.setPackagesToScan("org.springframework.batch.infrastructure.item.sample");
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
