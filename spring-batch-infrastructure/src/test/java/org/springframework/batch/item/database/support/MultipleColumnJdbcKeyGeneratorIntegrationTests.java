/**
 * 
 */
package org.springframework.batch.item.database.support;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

/**
 * @author Lucas Ward
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/data-source-context.xml")
@SuppressWarnings("deprecation")
public class MultipleColumnJdbcKeyGeneratorIntegrationTests {
	
	MultipleColumnJdbcKeyCollector<Map<?,?>> keyStrategy;
	
	ExecutionContext executionContext;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUpBeforeTransaction() throws Exception {

		keyStrategy = new MultipleColumnJdbcKeyCollector<Map<?,?>>(simpleJdbcTemplate.getJdbcOperations(),
		"SELECT ID, VALUE from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID, VALUE from T_FOOS where ID > ? and VALUE > ? order by ID");
		
		executionContext = new ExecutionContext();
	}
	
	@Transactional @Test
	public void testRetrieveKeys(){
		
		List<Map<?,?>> keys = keyStrategy.retrieveKeys(executionContext);
		
		for (int i = 0; i < keys.size(); i++) {
			Map<?,?> id = keys.get(i);
			assertEquals(i + 1L, id.get("ID"));
			assertEquals(i + 1, id.get("VALUE"));
		}
	}
	
	@Transactional @Test
	public void testRestoreKeys(){
		
		Map<String, String> keyMap = new LinkedHashMap<String, String>();
		keyMap.put("ID", "3");
		keyMap.put("VALUE", "3");
		executionContext.put(ClassUtils.getShortName(MultipleColumnJdbcKeyCollector.class)+ ".current.key", keyMap);
		
		List<Map<?, ?>> keys = keyStrategy.retrieveKeys(executionContext);
		
		assertEquals(2, keys.size());
		Map<?,?> key = keys.get(0);
		assertEquals(4L, key.get("ID"));
		assertEquals(4, key.get("VALUE"));
		key = keys.get(1);
		assertEquals(5L, key.get("ID"));
		assertEquals(5, key.get("VALUE"));
	}
	
	@Transactional @Test
	public void testGetNullKeyAsStreamContext(){
		
		try{
			keyStrategy.updateContext(null, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}

}
