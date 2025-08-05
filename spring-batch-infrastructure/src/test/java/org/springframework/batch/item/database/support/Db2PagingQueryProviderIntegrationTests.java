/*
 * Copyright 2024-2025 the original author or authors.
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

import com.ibm.db2.jcc.DB2SimpleDataSource;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * @author Henning Pöttker
 * @author Mahmoud Ben Hassine
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture.sql", executionPhase = BEFORE_TEST_CLASS)
@Disabled("https://github.com/spring-projects/spring-batch/issues/4828")
class Db2PagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName DB2_IMAGE = DockerImageName.parse("icr.io/db2_community/db2:12.1.0.0");

	@Container
	public static Db2Container db2 = new Db2Container(DB2_IMAGE).acceptLicense();

	Db2PagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new Db2PagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			DB2SimpleDataSource dataSource = new DB2SimpleDataSource();
			dataSource.setDatabaseName(db2.getDatabaseName());
			dataSource.setUser(db2.getUsername());
			dataSource.setPassword(db2.getPassword());
			dataSource.setDriverType(4);
			dataSource.setServerName(db2.getHost());
			dataSource.setPortNumber(db2.getMappedPort(Db2Container.DB2_PORT));
			dataSource.setSslConnection(false);
			return dataSource;
		}

	}

}
