/*
 * Copyright 2006-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class DatabaseTypeIntegrationTests {

	@Test
	public void testH2() throws Exception {
		DataSource dataSource = DatabaseTypeTestUtils.getDataSource(org.h2.Driver.class,
				"jdbc:h2:file:target/data/sample");
		assertEquals(DatabaseType.H2, DatabaseType.fromMetaData(dataSource));
		dataSource.getConnection();
	}

	@Test
	public void testDerby() throws Exception {
		DataSource dataSource = DatabaseTypeTestUtils.getDataSource(org.apache.derby.jdbc.EmbeddedDriver.class,
				"jdbc:derby:derby-home/test;create=true", "sa", "");
		assertEquals(DatabaseType.DERBY, DatabaseType.fromMetaData(dataSource));
		dataSource.getConnection();
	}

}
