/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.exlore.support;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * @author Lucas Ward
 * 
 */
public class JobExplorerFactoryBeanTests {

	private JobExplorerFactoryBean factory;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private DataSource dataSource;

	private String tablePrefix = "TEST_BATCH_PREFIX_";

	@Before
	public void setUp() throws Exception {

		factory = new JobExplorerFactoryBean();
		dataSource = createMock(DataSource.class);
		factory.setDataSource(dataSource);
		incrementerFactory = createMock(DataFieldMaxValueIncrementerFactory.class);
		factory.setIncrementerFactory(incrementerFactory);
		factory.setTablePrefix(tablePrefix);

	}

	@Test
	public void testDetectDatabaseType() throws Exception {

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		Connection con = createMock(Connection.class);
		expect(dataSource.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("Oracle");
		expect(incrementerFactory.isSupportedIncrementerType("ORACLE")).andReturn(true);
		expect(incrementerFactory.getSupportedIncrementerTypes()).andReturn(new String[0]);
		expect(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).andReturn(new StubIncrementer());
		expect(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).andReturn(
				new StubIncrementer());
		expect(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).andReturn(
				new StubIncrementer());
		replay(dataSource, con, dmd, incrementerFactory);
		factory.afterPropertiesSet();

	}

	@Test
	public void testNoDatabaseType() throws Exception {

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		Connection con = createMock(Connection.class);
		expect(dataSource.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("foo");
		try {
			expect(incrementerFactory.isSupportedIncrementerType(null)).andReturn(false);
			expect(incrementerFactory.getSupportedIncrementerTypes()).andReturn(new String[0]);
			replay(dataSource, con, dmd, incrementerFactory);
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("DatabaseType") >= 0);
		}

	}

	@Test
	public void testMissingDataSource() throws Exception {

		factory.setDataSource(null);
		try {
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("DataSource") >= 0);
		}

	}

	@Test
	public void testInvalidDatabaseType() throws Exception {

		factory.setDatabaseType("foo");
		try {
			expect(incrementerFactory.isSupportedIncrementerType("foo")).andReturn(false);
			expect(incrementerFactory.getSupportedIncrementerTypes()).andReturn(new String[0]);
			replay(incrementerFactory);
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("foo") >= 0);
		}

	}

	@Test
	public void testCreateExplorer() throws Exception {
		String databaseType = "foo";
		factory.setDatabaseType(databaseType);

		expect(incrementerFactory.isSupportedIncrementerType("foo")).andReturn(true);
		expect(incrementerFactory.getSupportedIncrementerTypes()).andReturn(new String[0]);
		expect(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ")).andReturn(
				new StubIncrementer());
		expect(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_EXECUTION_SEQ")).andReturn(
				new StubIncrementer());
		expect(incrementerFactory.getIncrementer(databaseType, tablePrefix + "STEP_EXECUTION_SEQ")).andReturn(
				new StubIncrementer());
		replay(incrementerFactory);

		factory.afterPropertiesSet();
		factory.getObject();

		verify(incrementerFactory);

	}

	private static class StubIncrementer implements DataFieldMaxValueIncrementer {

		public int nextIntValue() throws DataAccessException {
			return 0;
		}

		public long nextLongValue() throws DataAccessException {
			return 0;
		}

		public String nextStringValue() throws DataAccessException {
			return null;
		}

	}

}
