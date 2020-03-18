/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.test;

import javax.sql.DataSource;

import org.junit.Before;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * @author Mahmoud Ben Hassine
 */
public class AbstractIntegrationTests {

	protected DataSource dataSource;

	@Before
	public void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-drop-hsqldb.sql"));
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-hsqldb.sql"));
		databasePopulator.addScript(new ClassPathResource("/business-schema-hsqldb.sql"));
		databasePopulator.execute(this.dataSource);
	}

}
