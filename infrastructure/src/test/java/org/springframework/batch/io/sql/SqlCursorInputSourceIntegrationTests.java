package org.springframework.batch.io.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.batch.restart.RestartData;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * 
 * @author Lucas Ward
 *
 */
public class SqlCursorInputSourceIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected SqlCursorInputSource sqlCursorInputSource;
	
	private RowMapper mapper = new SqlRowMapper();
	
	private static final String CURRENT_PROCESSED_ROW = "sqlCursorInput.lastProcessedRowNum";
	private static final String SKIP_COUNT = "sqlCursorInput.skippedRrecordCount";
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}
	
	protected void onSetUp()throws Exception{
		sqlCursorInputSource = getNewInputSource();
		sqlCursorInputSource.setMapper(mapper);
		RepeatSynchronizationManager.register(new RepeatContextSupport(null));
		super.onSetUp();
	}
	
	protected void onTearDown()throws Exception{
		//cursor must be closed between each test, and transaction synchronization
		//list must be cleared.
		BatchTransactionSynchronizationManager.clearSynchronizations();
		RepeatSynchronizationManager.clear();
		sqlCursorInputSource.close();
		super.onTearDown();
	}
	
	protected SqlCursorInputSource getNewInputSource(){
		
		SqlCursorInputSource result = new SqlCursorInputSource();
		result.setDataSource(super.getJdbcTemplate().getDataSource());
		result.setSql("SELECT * from T_FOOS");
		return result;
	}
	
	public void testNormalReading(){
		
		int fooCount = 0;
		
		for(;;){
			Foo foo = (Foo)sqlCursorInputSource.read();
			
			if( foo == null){
				break;
			}
			
			fooCount++;
			validateFoo(fooCount, "bar" + fooCount, fooCount, foo);
			
		}
		
		assertEquals(5, fooCount );
	}
	
	public void testRestart(){
		
		sqlCursorInputSource.read();
		Foo foo = (Foo)sqlCursorInputSource.read();
		
		validateFoo(2, "bar2", 2, foo);
		
		RestartData restartData = sqlCursorInputSource.getRestartData();
		
		sqlCursorInputSource = getNewInputSource();
		sqlCursorInputSource.setMapper(mapper);
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(1, "bar1", 1, foo);
		
		sqlCursorInputSource.restoreFrom(restartData);
		
		foo = (Foo)sqlCursorInputSource.read();
		
		validateFoo(3, "bar3", 3, foo);
	}
	
	public void testReadWithNullMapper(){
		
		//calling read without a mapper should throw an exception.
		sqlCursorInputSource.setMapper(null);
		try{
			sqlCursorInputSource.read();
			fail();
		}
		catch(IllegalStateException ex){
			//expected
		}
	}
	
	public void testStatistics(){
		
		Properties statistics = sqlCursorInputSource.getStatistics();
		assertEquals("0", statistics.getProperty(CURRENT_PROCESSED_ROW));
		sqlCursorInputSource.read();
		
		statistics = sqlCursorInputSource.getStatistics();
		assertEquals("1", statistics.getProperty(CURRENT_PROCESSED_ROW));
	}
	
	public void testSkipCountStatistics(){
		
		sqlCursorInputSource.read();
		Foo foo = (Foo)sqlCursorInputSource.read();
		validateFoo(2, "bar2", 2, foo);
		
		Properties statistics = sqlCursorInputSource.getStatistics();
		assertEquals("0", statistics.getProperty(SKIP_COUNT));
		
		sqlCursorInputSource.skip();
		
		statistics = sqlCursorInputSource.getStatistics();
		assertEquals("1", statistics.getProperty(SKIP_COUNT));
		
		sqlCursorInputSource.read();
		
		sqlCursorInputSource.read();
		sqlCursorInputSource.skip();
		
		statistics = sqlCursorInputSource.getStatistics();
		assertEquals("2", statistics.getProperty(SKIP_COUNT));
		
		super.endTransaction();
		super.startNewTransaction();
		
		sqlCursorInputSource.read();
		
		statistics = sqlCursorInputSource.getStatistics();
		assertEquals("2", statistics.getProperty(SKIP_COUNT));
	}
	
	public void testRollback(){
		
		sqlCursorInputSource.read();
		Foo foo = (Foo)sqlCursorInputSource.read();
		validateFoo(2, "bar2", 2, foo);
		
		super.setComplete();
		super.endTransaction();
		super.startNewTransaction();
		BatchTransactionSynchronizationManager.resynchronize();
		
		sqlCursorInputSource.read();
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(4, "bar4", 4, foo);
		
		super.endTransaction();
		super.startNewTransaction();
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(3, "bar3", 3, foo);
	}
	
	public void testSkip(){
		
		sqlCursorInputSource.read();
		Foo foo = (Foo)sqlCursorInputSource.read();
		validateFoo(2, "bar2", 2, foo);
		
		sqlCursorInputSource.skip();
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(3, "bar3", 3, foo);
		
		super.endTransaction();
		super.startNewTransaction();
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(1, "bar1", 1, foo);
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(3, "bar3", 3, foo);
	}
	
	public void testSucessiveSkip(){
		
		sqlCursorInputSource.read();
		Foo foo = (Foo)sqlCursorInputSource.read();
		validateFoo(2, "bar2", 2, foo);
		sqlCursorInputSource.skip();
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(3, "bar3", 3, foo);
		sqlCursorInputSource.skip();
		
		super.endTransaction();
		super.startNewTransaction();
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(1, "bar1", 1, foo);
		
		foo = (Foo)sqlCursorInputSource.read();
		validateFoo(4, "bar4", 4, foo);
	}
	
	private void validateFoo(int id, String name, int value, Foo foo){
		
		assertEquals(id, foo.id);
		assertEquals(name, foo.name);
		assertEquals(value, foo.value);
	}
	
	private class SqlRowMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			Foo foo = new Foo();
			foo.id = rs.getInt(1);
			foo.name = rs.getString(2);
			foo.value = rs.getInt(3);
			
			return foo;
		}
		
	}
	
	private class Foo{
		
		private int id;
		private String name;
		private int value;
	}
}
