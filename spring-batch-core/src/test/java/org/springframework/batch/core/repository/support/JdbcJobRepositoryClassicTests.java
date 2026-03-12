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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Repository tests using JDBC DAOs and classic schema.
 *
 * @author Thomas Risberg
 **/
@SpringJUnitConfig(classes = { JdbcJobRepositoryClassicTestConfiguration.class })
@DirtiesContext
public class JdbcJobRepositoryClassicTests extends AbstractJobRepositoryIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	public void setUp() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new FileSystemResource(
					"src/test/resources/org/springframework/batch/core/schema-drop-classic-hsqldb.sql"));
			ScriptUtils.executeSqlScript(connection, new FileSystemResource(
					"src/test/resources/org/springframework/batch/core/schema-classic-hsqldb.sql"));
		}
	}

	@AfterEach
	public void cleanUp() throws Exception {
		System.setProperty("spring.batch.jdbc.schema.classic", "false");
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new FileSystemResource(
					"src/test/resources/org/springframework/batch/core/schema-drop-classic-hsqldb.sql"));
		}
	}

}
