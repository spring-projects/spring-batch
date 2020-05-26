/*
 * Copyright 2006-2017 the original author or authors.
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
package org.springframework.batch.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
public class DatabaseTypeTestUtils {
	
	public static DataSource getDataSource(Class<?> driver, String url, String username, String password) throws Exception {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driver.getName());
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
	
	public static DataSource getDataSource(Class<?> driver, String url) throws Exception {
		return getDataSource(driver, url, null, null);
	}

	public static DataSource getMockDataSource() throws Exception {
		return getMockDataSource(DatabaseType.HSQL.getProductName());
	}

	public static DataSource getMockDataSource(String databaseProductName) throws Exception {
		return getMockDataSource(databaseProductName, null);
	}

	public static DataSource getMockDataSource(String databaseProductName, String databaseVersion) throws Exception {
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(ds.getConnection()).thenReturn(con);
		when(con.getMetaData()).thenReturn(dmd);
		when(dmd.getDatabaseProductName()).thenReturn(databaseProductName);
		if (databaseVersion!=null) {
			when(dmd.getDatabaseProductVersion()).thenReturn(databaseVersion);
		}
		return ds;
	}

	public static DataSource getMockDataSource(Exception e) throws Exception {
		DataSource ds = mock(DataSource.class);
		when(ds.getConnection()).thenReturn(null);
		return ds;
	}

}
