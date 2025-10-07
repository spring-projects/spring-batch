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
package org.springframework.batch.infrastructure.item.database.support;

import java.nio.file.Path;
import javax.sql.DataSource;

import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.sqlite.SQLiteDataSource;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * @author Henning Pöttker
 */
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture.sql", executionPhase = BEFORE_TEST_CLASS)
class SqlitePagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	@TempDir
	private static Path TEMP_DIR;

	SqlitePagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new SqlitePagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			SQLiteDataSource dataSource = new SQLiteDataSource();
			dataSource.setUrl("jdbc:sqlite:" + TEMP_DIR.resolve("spring-batch.sqlite"));
			return dataSource;
		}

	}

}
