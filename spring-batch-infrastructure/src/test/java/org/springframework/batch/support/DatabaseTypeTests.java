package org.springframework.batch.support;

import static org.springframework.batch.support.DatabaseType.*;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Test;

/**
 * 
 * @author Lucas Ward
 *
 */
public class DatabaseTypeTests {

	@Test
	public void testFromProductName(){
		assertEquals(DERBY, fromProductName("Apache Derby"));
		assertEquals(DB2, fromProductName("DB2"));
		assertEquals(DB2ZOS, fromProductName("DB2ZOS"));
		assertEquals(HSQL, fromProductName("HSQL Database Engine"));
		assertEquals(SQLSERVER, fromProductName("Microsoft SQL Server"));
		assertEquals(MYSQL, fromProductName("MySQL"));
		assertEquals(ORACLE, fromProductName("Oracle"));
		assertEquals(POSTGRES, fromProductName("PostgreSQL"));
		assertEquals(SYBASE, fromProductName("Sybase"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidProductName(){
		
		fromProductName("bad product name");
	}
	
	@Test
	public void testFromMetaDataForDerby() throws Exception{
		
		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("Apache Derby");
		replay(dmd,ds,con);
		
		assertEquals(DERBY, DatabaseType.fromMetaData(ds));
		
		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForDB2() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("DB2/Linux");
		replay(dmd,ds,con);

		assertEquals(DB2, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForDB2ZOS() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("DB2");
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductVersion()).andReturn("DSN08015");
		replay(dmd,ds,con);

		assertEquals(DB2ZOS, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForHsql() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("HSQL Database Engine");
		replay(dmd,ds,con);

		assertEquals(HSQL, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForSqlServer() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("Microsoft SQL Server");
		replay(dmd,ds,con);

		assertEquals(SQLSERVER, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForMySql() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("MySQL");
		replay(dmd,ds,con);

		assertEquals(MYSQL, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForOracle() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("Oracle");
		replay(dmd,ds,con);

		assertEquals(ORACLE, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForPostgres() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("PostgreSQL");
		replay(dmd,ds,con);

		assertEquals(POSTGRES, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}

	@Test
	public void testFromMetaDataForSybase() throws Exception{

		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(ds.getConnection()).andReturn(con);
		expect(con.getMetaData()).andReturn(dmd);
		expect(dmd.getDatabaseProductName()).andReturn("Adaptive Server Enterprise");
		replay(dmd,ds,con);

		assertEquals(SYBASE, DatabaseType.fromMetaData(ds));

		verify(dmd,ds,con);
	}
}
