/*
 * Copyright 2008-2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.springframework.batch.support.DatabaseType.DB2;
import static org.springframework.batch.support.DatabaseType.DB2VSE;
import static org.springframework.batch.support.DatabaseType.DB2ZOS;
import static org.springframework.batch.support.DatabaseType.DB2AS400;
import static org.springframework.batch.support.DatabaseType.DERBY;
import static org.springframework.batch.support.DatabaseType.HSQL;
import static org.springframework.batch.support.DatabaseType.MYSQL;
import static org.springframework.batch.support.DatabaseType.ORACLE;
import static org.springframework.batch.support.DatabaseType.POSTGRES;
import static org.springframework.batch.support.DatabaseType.SQLITE;
import static org.springframework.batch.support.DatabaseType.SQLSERVER;
import static org.springframework.batch.support.DatabaseType.SYBASE;
import static org.springframework.batch.support.DatabaseType.fromProductName;

/**
 * 
 * @author Lucas Ward
 * @author Will Schipp
 * 
 */
public class DatabaseTypeTests {

	@Test
	public void testFromProductName() {
		assertEquals(DERBY, fromProductName("Apache Derby"));
		assertEquals(DB2, fromProductName("DB2"));
		assertEquals(DB2VSE, fromProductName("DB2VSE"));
		assertEquals(DB2ZOS, fromProductName("DB2ZOS"));
		assertEquals(DB2AS400, fromProductName("DB2AS400"));
		assertEquals(HSQL, fromProductName("HSQL Database Engine"));
		assertEquals(SQLSERVER, fromProductName("Microsoft SQL Server"));
		assertEquals(MYSQL, fromProductName("MySQL"));
		assertEquals(ORACLE, fromProductName("Oracle"));
		assertEquals(POSTGRES, fromProductName("PostgreSQL"));
		assertEquals(SYBASE, fromProductName("Sybase"));
		assertEquals(SQLITE, fromProductName("SQLite"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidProductName() {

		fromProductName("bad product name");
	}

	@Test
	public void testFromMetaDataForDerby() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("Apache Derby");
		assertEquals(DERBY, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForDB2() throws Exception {
		DataSource oldDs = DatabaseTypeTestUtils.getMockDataSource("DB2/Linux", "SQL0901");
		assertEquals(DB2, DatabaseType.fromMetaData(oldDs));

		DataSource newDs = DatabaseTypeTestUtils.getMockDataSource("DB2/NT", "SQL0901");
		assertEquals(DB2, DatabaseType.fromMetaData(newDs));
	}

	@Test
	public void testFromMetaDataForDB2VSE() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("DB2 for DB2 for z/OS VUE", "ARI08015");
		assertEquals(DB2VSE, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForDB2ZOS() throws Exception {
		DataSource oldDs = DatabaseTypeTestUtils.getMockDataSource("DB2", "DSN08015");
		assertEquals(DB2ZOS, DatabaseType.fromMetaData(oldDs));

		DataSource newDs = DatabaseTypeTestUtils.getMockDataSource("DB2 for DB2 UDB for z/OS", "DSN08015");
		assertEquals(DB2ZOS, DatabaseType.fromMetaData(newDs));
	}

	@Test
	public void testFromMetaDataForDB2AS400() throws Exception {
		DataSource toolboxDs = DatabaseTypeTestUtils.getMockDataSource("DB2 UDB for AS/400", "07.01.0000 V7R1m0");
		assertEquals(DB2AS400, DatabaseType.fromMetaData(toolboxDs));

		DataSource nativeDs = DatabaseTypeTestUtils.getMockDataSource("DB2 UDB for AS/400", "V7R1M0");
		assertEquals(DB2AS400, DatabaseType.fromMetaData(nativeDs));

		DataSource prdidDs = DatabaseTypeTestUtils.getMockDataSource("DB2 UDB for AS/400", "QSQ07010");
		assertEquals(DB2AS400, DatabaseType.fromMetaData(prdidDs));
	}

	@Test
	public void testFromMetaDataForHsql() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("HSQL Database Engine");
		assertEquals(HSQL, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForSqlServer() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("Microsoft SQL Server");
		assertEquals(SQLSERVER, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForMySql() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("MySQL");
		assertEquals(MYSQL, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForOracle() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("Oracle");
		assertEquals(ORACLE, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForPostgres() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("PostgreSQL");
		assertEquals(POSTGRES, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForSybase() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("Adaptive Server Enterprise");
		assertEquals(SYBASE, DatabaseType.fromMetaData(ds));
	}

	@Test(expected=MetaDataAccessException.class)
	public void testBadMetaData() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource(new MetaDataAccessException("Bad!"));
		assertEquals(SYBASE, DatabaseType.fromMetaData(ds));
	}

}
