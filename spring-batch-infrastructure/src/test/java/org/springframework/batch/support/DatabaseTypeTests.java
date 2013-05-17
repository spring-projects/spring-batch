package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;
import static org.springframework.batch.support.DatabaseType.DB2;
import static org.springframework.batch.support.DatabaseType.DB2ZOS;
import static org.springframework.batch.support.DatabaseType.DERBY;
import static org.springframework.batch.support.DatabaseType.HSQL;
import static org.springframework.batch.support.DatabaseType.MYSQL;
import static org.springframework.batch.support.DatabaseType.ORACLE;
import static org.springframework.batch.support.DatabaseType.POSTGRES;
import static org.springframework.batch.support.DatabaseType.SQLSERVER;
import static org.springframework.batch.support.DatabaseType.SYBASE;
import static org.springframework.batch.support.DatabaseType.TIMESTEN;
import static org.springframework.batch.support.DatabaseType.fromProductName;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.jdbc.support.MetaDataAccessException;

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
		assertEquals(DB2ZOS, fromProductName("DB2ZOS"));
		assertEquals(HSQL, fromProductName("HSQL Database Engine"));
		assertEquals(SQLSERVER, fromProductName("Microsoft SQL Server"));
		assertEquals(MYSQL, fromProductName("MySQL"));
		assertEquals(ORACLE, fromProductName("Oracle"));
		assertEquals(TIMESTEN, fromProductName("TimesTen"));
		assertEquals(POSTGRES, fromProductName("PostgreSQL"));
		assertEquals(SYBASE, fromProductName("Sybase"));
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
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("DB2/Linux");
		assertEquals(DB2, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForDB2ZOS() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("DB2", "DSN08015");
		assertEquals(DB2ZOS, DatabaseType.fromMetaData(ds));
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
	public void testFromMetaDataForTimesTen() throws Exception {
		DataSource ds = DatabaseTypeTestUtils.getMockDataSource("TimesTen");
		assertEquals(TIMESTEN, DatabaseType.fromMetaData(ds));
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
