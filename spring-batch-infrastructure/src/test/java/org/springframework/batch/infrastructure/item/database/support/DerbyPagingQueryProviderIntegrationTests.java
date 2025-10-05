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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Henning Pöttker
 */
@SpringJUnitConfig
class DerbyPagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	DerbyPagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new DerbyPagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.DERBY)
				.addScript("/org/springframework/batch/infrastructure/item/database/support/query-provider-fixture.sql")
				.generateUniqueName(true)
				.build();
		}

	}

}
