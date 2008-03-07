package org.springframework.batch.item.database.support;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyGenerator;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * 
 * @author Lucas Ward
 *
 */
public class SingleColumnJdbcKeyGeneratorIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	SingleColumnJdbcKeyGenerator keyStrategy;
	
	ExecutionContext executionContext;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/item/database/data-source-context.xml"};
	}

	
	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		keyStrategy = new SingleColumnJdbcKeyGenerator(getJdbcTemplate(),
		"SELECT ID from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		
		executionContext = new ExecutionContext();
	}
	
	public void testRetrieveKeys(){
		
		List keys = keyStrategy.retrieveKeys(new ExecutionContext());
		
		for (int i = 0; i < keys.size(); i++) {
			Long id = (Long)keys.get(i);
			assertEquals(new Long(i + 1), id);
		}
	}
	
	public void testRestoreKeys(){
		
		executionContext.putString("key", "3");
		
		List keys = keyStrategy.retrieveKeys(executionContext);
		
		assertEquals(2, keys.size());
		assertEquals(new Long(4), keys.get(0));
		assertEquals(new Long(5), keys.get(1));
	}
	
	public void testGetKeyAsStreamContext(){
		
		keyStrategy.saveState(new Long(3), executionContext);
		
		assertEquals(1, executionContext.size());
		assertEquals("3", executionContext.getString("key"));
	}
	
	public void testGetNullKeyAsStreamContext(){
		
		try{
			keyStrategy.saveState(null, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testRestoreKeysFromNull(){
		
		try{
			keyStrategy.saveState(null, null);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}
