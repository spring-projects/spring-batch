/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * @author Henning Pöttker
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture.sql", executionPhase = BEFORE_TEST_CLASS)
class MySqlPagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.31");

	@Container
	public static MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE);

	MySqlPagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new MySqlPagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			MysqlDataSource datasource = new MysqlDataSource();
			datasource.setURL(mysql.getJdbcUrl());
			datasource.setUser(mysql.getUsername());
			datasource.setPassword(mysql.getPassword());
			datasource.setUseSSL(false);
			return datasource;
		}

	}

}
