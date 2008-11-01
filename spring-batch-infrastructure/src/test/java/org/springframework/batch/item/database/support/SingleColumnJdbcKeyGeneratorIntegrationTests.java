package org.springframework.batch.item.database.support;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * 
 * @author Lucas Ward
 *
 */
public class SingleColumnJdbcKeyGeneratorIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	SingleColumnJdbcKeyCollector keyStrategy;
	
	ExecutionContext executionContext;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/item/database/data-source-context.xml"};
	}

	
	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		keyStrategy = new SingleColumnJdbcKeyCollector(getJdbcTemplate(),
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
		for (int i = 0; i < keys.size(); i++) {
			System.out.println(keys.get(i));
		}
	
	}
	
	public void testRestoreKeys(){
		
		keyStrategy.updateContext(new Long(3), executionContext);
		
		List keys = keyStrategy.retrieveKeys(executionContext);
		
		assertEquals(2, keys.size());
		assertEquals(new Long(4), keys.get(0));
		assertEquals(new Long(5), keys.get(1));
		
		for (int i = 0; i < keys.size(); i++) {
			System.out.println(keys.get(i));
		}
	}
	
	public void testGetKeyAsStreamContext(){
		
		keyStrategy.updateContext(new Long(3), executionContext);
		
		assertEquals(1, executionContext.size());
		assertEquals(new Long(3), executionContext.get(ClassUtils.getShortName(SingleColumnJdbcKeyCollector.class) + ".key"));
	}
	
	public void testGetNullKeyAsStreamContext(){
		
		try{
			keyStrategy.updateContext(null, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testRestoreKeysFromNull(){
		
		try{
			keyStrategy.updateContext(null, null);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}
