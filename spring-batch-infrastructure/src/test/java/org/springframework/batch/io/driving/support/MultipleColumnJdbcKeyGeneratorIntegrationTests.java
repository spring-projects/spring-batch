/**
 * 
 */
package org.springframework.batch.io.driving.support;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.core.CollectionFactory;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * @author Lucas Ward
 *
 */
public class MultipleColumnJdbcKeyGeneratorIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {
	
	MultipleColumnJdbcKeyGenerator keyStrategy;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		keyStrategy = new MultipleColumnJdbcKeyGenerator(getJdbcTemplate(),
		"SELECT ID, VALUE from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID, VALUE from T_FOOS where ID > ? and VALUE > ? order by ID");
	}
	
	public void testRetrieveKeys(){
		
		List keys = keyStrategy.retrieveKeys();
		
		for (int i = 0; i < keys.size(); i++) {
			Map id = (Map)keys.get(i);
			assertEquals(id.get("ID"), new Long(i + 1));
			assertEquals(id.get("VALUE"), new Integer(i + 1));
		}
	}
	
	public void testRestoreKeys(){
		
		Properties props = new Properties();
		props.setProperty(ColumnMapRestartDataRowMapper.KEY + "0", "3");
		props.setProperty(ColumnMapRestartDataRowMapper.KEY + "1", "3");
		StreamContext streamContext = new GenericStreamContext(props);
		
		List keys = keyStrategy.restoreKeys(streamContext);
		
		assertEquals(2, keys.size());
		Map key = (Map)keys.get(0);
		assertEquals(new Long(4), key.get("ID"));
		assertEquals(new Integer(4), key.get("VALUE"));
		key = (Map)keys.get(1);
		assertEquals(new Long(5), key.get("ID"));
		assertEquals(new Integer(5), key.get("VALUE"));
	}
	
	public void testGetKeyAsRestartData(){
		
		Map key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(1);
		key.put("ID", new Long(3));
		key.put("VALUE", new Integer(3));
		
		StreamContext streamContext = keyStrategy.getKeyAsRestartData(key);
		Properties props = streamContext.getProperties();
		
		assertEquals(2, props.size());
		assertEquals("3", props.get(ColumnMapRestartDataRowMapper.KEY + "0"));
		assertEquals("3", props.get(ColumnMapRestartDataRowMapper.KEY + "1"));
	}
	
	public void testGetNullKeyAsRestartData(){
		
		try{
			keyStrategy.getKeyAsRestartData(null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testRestoreKeysFromNull(){
		
		try{
			keyStrategy.getKeyAsRestartData(null);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}
