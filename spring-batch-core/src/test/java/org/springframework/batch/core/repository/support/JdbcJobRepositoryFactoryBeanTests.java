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
package org.springframework.batch.core.repository.support;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

class JdbcJobRepositoryFactoryBeanTests {

	private final List<EmbeddedDatabase> dataSources = new ArrayList<>();

	@AfterEach
	void tearDown() {
		this.dataSources.forEach(EmbeddedDatabase::shutdown);
	}

	@Test
	void schemaValidationIsDisabledByDefault() {
		DataSource dataSource = createDataSource(false);
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource);

		Assertions.assertDoesNotThrow(factoryBean::afterPropertiesSet);
	}

	@Test
	void schemaValidationSucceedsWhenMetadataTablesExist() {
		DataSource dataSource = createDataSource(true);
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource);
		factoryBean.setValidateSchema(true);

		Assertions.assertDoesNotThrow(factoryBean::afterPropertiesSet);
	}

	@Test
	void schemaValidationFailsWithConfiguredTablePrefixWhenMetadataTablesAreMissing() {
		DataSource dataSource = createDataSource(true);
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource);
		factoryBean.setTablePrefix("CUSTOM_");
		factoryBean.setValidateSchema(true);

		BatchConfigurationException exception = Assertions.assertThrows(BatchConfigurationException.class,
				factoryBean::afterPropertiesSet);

		Assertions.assertTrue(exception.getMessage().contains("CUSTOM_JOB_INSTANCE"));
		Assertions.assertTrue(exception.getMessage().contains("schema is initialized"));
	}

	private JdbcJobRepositoryFactoryBean createFactoryBean(DataSource dataSource) {
		JdbcJobRepositoryFactoryBean factoryBean = new JdbcJobRepositoryFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(new JdbcTransactionManager(dataSource));
		return factoryBean;
	}

	private DataSource createDataSource(boolean initializeSchema) {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
			.generateUniqueName(true);
		if (initializeSchema) {
			builder.addScript(DatabaseType.HSQL.getProductSchema());
		}
		EmbeddedDatabase dataSource = builder.build();
		this.dataSources.add(dataSource);
		return dataSource;
	}

}
