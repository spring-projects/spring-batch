/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.repository.support;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Repository tests using JDBC DAOs and legacy schema.
 *
 * @author Thomas Risberg
 * @author Mahmoud Ben Hassine
 **/
@SpringJUnitConfig
@DirtiesContext
public class JdbcJobRepositoryLegacySchemaIntegrationTests extends AbstractJobRepositoryIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	public void setUp() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection,
					new FileSystemResource("src/test/resources/schema-drop-hsqldb-legacy.sql"));
			ScriptUtils.executeSqlScript(connection,
					new FileSystemResource("src/test/resources/schema-hsqldb-legacy.sql"));
		}
	}

	@AfterEach
	public void cleanUp() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection,
					new FileSystemResource("src/test/resources/schema-drop-hsqldb-legacy.sql"));
		}
		System.clearProperty("spring.batch.jdbc.schema.legacy");
	}

	@Configuration
	@EnableJdbcJobRepository
	static class JdbcJobRepositoryLegacyTestConfiguration {

		@Bean
		public DataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
			dataSource.setUrl("jdbc:hsqldb:mem:test;sql.enforce_strict_size=true;hsqldb.tx=mvcc");
			dataSource.setUsername("sa");
			dataSource.setPassword("");
			return dataSource;
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public JdbcJobRepositoryFactoryBean jobRepository(DataSource dataSource,
				JdbcTransactionManager transactionManager) throws Exception {
			System.setProperty("spring.batch.jdbc.schema.legacy", "true");
			JdbcJobRepositoryFactoryBean jobRepositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
			jobRepositoryFactoryBean.setDataSource(dataSource);
			jobRepositoryFactoryBean.setTransactionManager(transactionManager);
			return jobRepositoryFactoryBean;
		}

	}

}
