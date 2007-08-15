package org.springframework.batch.io.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.RestartData;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public class SingleKeySqlDrivingQueryInputSourceIntegrationTests 
	extends AbstractTransactionalDataSourceSpringContextTests {

	SingleKeySqlDrivingQueryInputSource sqlInputSource;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}
	
	protected void onSetUp()throws Exception{
		super.onSetUp();
		
		sqlInputSource = createInputSource();
	}
	
	protected SingleKeySqlDrivingQueryInputSource createInputSource(){
		
		SingleKeySqlDrivingQueryInputSource inputSource = new SingleKeySqlDrivingQueryInputSource();
		inputSource.setDrivingQuery("SELECT ID from T_FOOS order by ID");
		inputSource.setDetailsQuery("SELECT NAME, VALUE from T_FOOS where ID = ?");
		inputSource.setRestartQuery("SELECT ID from T_FOOS where ID > ? order by ID");
		inputSource.setMapper(new FooMapper());
		inputSource.setDataSource(super.getJdbcTemplate().getDataSource());
		return inputSource;
	}
	
	protected void onTearDown()throws Exception{

		BatchTransactionSynchronizationManager.clearSynchronizations();
		sqlInputSource.close();
		super.onTearDown();
	}

	public void testNormalProcessing(){
		
		Foo foo = (Foo)sqlInputSource.read();
		assertEquals(1, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(2, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(3, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(4, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(5, foo.value);
		
		assertNull(sqlInputSource.read());
	}
	
/*	public void testRollback(){
		
		SqlIdentityKey key = sqlInputSource.readKey();
		assertEquals("1", key.getKeyValue("id"));
		
		key = sqlInputSource.readKey();
		assertEquals("2", key.getKeyValue("id"));
		
		super.setComplete();
		super.endTransaction();
		super.startNewTransaction();
		BatchTransactionSynchronizationManager.resynchronize();
		
		key = sqlInputSource.readKey();
		assertEquals("3", key.getKeyValue("id"));
		
		key = sqlInputSource.readKey();
		assertEquals("4", key.getKeyValue("id"));
		
		super.endTransaction();
		super.startNewTransaction();
		
		key = sqlInputSource.readKey();
		assertEquals("3", key.getKeyValue("id"));
		
	}*/
	
	public void testRestart(){
		
		Foo foo = (Foo)sqlInputSource.read();
		assertEquals(1, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(2, foo.value);
		
		RestartData restartData = sqlInputSource.getRestartData();
		
		//create new input source
		sqlInputSource = createInputSource();
		
		sqlInputSource.restoreFrom(restartData);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(3, foo.value);
	}
	
	//test that reading from an input source and then trying to restore causes an error.
	public void testInvalidRestore(){
		
		Foo foo = (Foo)sqlInputSource.read();
		assertEquals(1, foo.value);
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(2, foo.value);
		
		RestartData restartData = sqlInputSource.getRestartData();
		
		//create new input source
		sqlInputSource = createInputSource();
		
		foo = (Foo)sqlInputSource.read();
		assertEquals(1, foo.value);
		
		try{
			sqlInputSource.restoreFrom(restartData);
			fail();
		}
		catch(IllegalStateException ex){
			//expected
		}
	}
	
	private class Foo {
		String name;
		int value;
	}
	
	private class FooMapper implements RowMapper{

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			Foo foo = new Foo();
			foo.name = rs.getString(1);
			foo.value = rs.getInt(2);
			return foo;
		}
		
	}
}
