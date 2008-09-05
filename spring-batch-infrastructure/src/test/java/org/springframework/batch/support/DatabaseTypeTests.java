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
	public void testFromMetaData() throws Exception{
		
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
}
