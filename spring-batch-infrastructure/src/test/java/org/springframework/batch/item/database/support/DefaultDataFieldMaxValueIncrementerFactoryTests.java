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
package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;
import org.springframework.jdbc.support.incrementer.DB2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SqlServerMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SybaseMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DB2MainframeSequenceMaxValueIncrementer;

/**
 * @author Lucas Ward
 *
 */
public class DefaultDataFieldMaxValueIncrementerFactoryTests extends TestCase {

	private DefaultDataFieldMaxValueIncrementerFactory factory;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		DataSource dataSource = createMock(DataSource.class);
		factory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
	}
	
	public void testSupportedDatabaseType(){
		assertTrue(factory.isSupportedIncrementerType("db2"));
		assertTrue(factory.isSupportedIncrementerType("db2zos"));
		assertTrue(factory.isSupportedIncrementerType("mysql"));
		assertTrue(factory.isSupportedIncrementerType("derby"));
		assertTrue(factory.isSupportedIncrementerType("oracle"));
		assertTrue(factory.isSupportedIncrementerType("postgres"));
		assertTrue(factory.isSupportedIncrementerType("hsql"));
		assertTrue(factory.isSupportedIncrementerType("sqlserver"));
		assertTrue(factory.isSupportedIncrementerType("sybase"));
	}
	
	public void testUnsupportedDatabaseType(){
		assertFalse(factory.isSupportedIncrementerType("invalidtype"));
	}
	
	public void testInvalidDatabaseType(){
		try{
			factory.getIncrementer("invalidtype", "NAME");
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testNullIncrementerName(){
		try{
			factory.getIncrementer("db2", null);
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testDb2(){
		assertTrue(factory.getIncrementer("db2", "NAME") instanceof DB2SequenceMaxValueIncrementer);
	}
	
	public void testDb2zos(){
		assertTrue(factory.getIncrementer("db2zos", "NAME") instanceof DB2MainframeSequenceMaxValueIncrementer);
	}

	public void testMysql(){
		assertTrue(factory.getIncrementer("mysql", "NAME") instanceof MySQLMaxValueIncrementer);
	}

	public void testOracle(){
		factory.setIncrementerColumnName("ID");
		assertTrue(factory.getIncrementer("oracle", "NAME") instanceof OracleSequenceMaxValueIncrementer);
	}

	public void testDerby(){
		assertTrue(factory.getIncrementer("derby", "NAME") instanceof DerbyMaxValueIncrementer);
	}

	public void testHsql(){
		assertTrue(factory.getIncrementer("hsql", "NAME") instanceof HsqlMaxValueIncrementer);
	}
	
	public void testPostgres(){
		assertTrue(factory.getIncrementer("postgres", "NAME") instanceof PostgreSQLSequenceMaxValueIncrementer);
	}

	public void testMsSqlServer(){
		assertTrue(factory.getIncrementer("sqlserver", "NAME") instanceof SqlServerMaxValueIncrementer);
	}

	public void testSybase(){
		assertTrue(factory.getIncrementer("sybase", "NAME") instanceof SybaseMaxValueIncrementer);
	}


}
