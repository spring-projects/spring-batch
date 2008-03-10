/**
 * 
 */
package org.springframework.batch.item.database.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.CollectionFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * @author Lucas Ward
 *
 */
public class MultipleColumnJdbcKeyGeneratorIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {
	
	MultipleColumnJdbcKeyCollector keyStrategy;
	
	ExecutionContext executionContext;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/item/database/data-source-context.xml"};
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		keyStrategy = new MultipleColumnJdbcKeyCollector(getJdbcTemplate(),
		"SELECT ID, VALUE from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID, VALUE from T_FOOS where ID > ? and VALUE > ? order by ID");
		
		executionContext = new ExecutionContext();
	}
	
	public void testRetrieveKeys(){
		
		List keys = keyStrategy.retrieveKeys(executionContext);
		
		for (int i = 0; i < keys.size(); i++) {
			Map id = (Map)keys.get(i);
			assertEquals(id.get("ID"), new Long(i + 1));
			assertEquals(id.get("VALUE"), new Integer(i + 1));
		}
	}
	
	public void testRestoreKeys(){
		
		executionContext.putString(ColumnMapExecutionContextRowMapper.KEY_PREFIX + "0", "3");
		executionContext.putString(ColumnMapExecutionContextRowMapper.KEY_PREFIX + "1", "3");
		
		List keys = keyStrategy.retrieveKeys(executionContext);
		
		assertEquals(2, keys.size());
		Map key = (Map)keys.get(0);
		assertEquals(new Long(4), key.get("ID"));
		assertEquals(new Integer(4), key.get("VALUE"));
		key = (Map)keys.get(1);
		assertEquals(new Long(5), key.get("ID"));
		assertEquals(new Integer(5), key.get("VALUE"));
	}
	
	public void testGetKeyAsExecutionContext(){
		
		Map key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(1);
		key.put("ID", new Long(3));
		key.put("VALUE", new Integer(4));
		
		keyStrategy.setKeyMapper(new ExecutionContextRowMapper() {
			public PreparedStatementSetter createSetter(ExecutionContext executionContext) {
				return null;
			}
			public void mapKeys(Object key, ExecutionContext executionContext) {
				// Just slap the key as a map into the context
				Map keys = (Map) key;
				for (Iterator it = keys.entrySet().iterator(); it.hasNext();) {
					Entry entry = (Entry)it.next();
					executionContext.put(entry.getKey().toString(), entry.getValue());
				}
			}
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}
		});
		keyStrategy.saveState(key, executionContext);
		Properties props = executionContext.getProperties();
		
		assertEquals(2, props.size());
		System.err.println(props);
		assertEquals("3", props.get("ID"));
		assertEquals("4", props.get("VALUE"));
	}
	
	public void testGetNullKeyAsStreamContext(){
		
		try{
			keyStrategy.saveState(null, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}
