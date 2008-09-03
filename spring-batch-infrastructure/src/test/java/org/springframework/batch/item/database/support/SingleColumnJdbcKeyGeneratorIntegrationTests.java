package org.springframework.batch.item.database.support;

import static org.junit.Assert.*;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.ClassUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

/**
 * 
 * @author Lucas Ward
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/data-source-context.xml")
@SuppressWarnings("deprecation")
public class SingleColumnJdbcKeyGeneratorIntegrationTests {

	SingleColumnJdbcKeyCollector<Long> keyStrategy;
	
	ExecutionContext executionContext;
	
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUpBeforeTransaction() throws Exception {

		keyStrategy = new SingleColumnJdbcKeyCollector<Long>(simpleJdbcTemplate.getJdbcOperations(),
		"SELECT ID from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		
		executionContext = new ExecutionContext();
	}
	
	@Transactional @Test
	public void testRetrieveKeys(){
		
		List<Long> keys = keyStrategy.retrieveKeys(new ExecutionContext());
		
		for (int i = 0; i < keys.size(); i++) {
			Long id = keys.get(i);
			assertEquals(new Long(i + 1), id);
		}
		for (Long key : keys) {
			System.out.println(key);
		}

	}
	
	@Transactional @Test
	public void testRestoreKeys(){
		
		keyStrategy.updateContext(3L, executionContext);
		
		List<Long> keys = keyStrategy.retrieveKeys(executionContext);
		
		assertEquals(2, keys.size());
		assertEquals(new Long(4), keys.get(0));
		assertEquals(new Long(5), keys.get(1));

		for (Long key : keys) {
			System.out.println(key);
		}
	}
	
	@Transactional @Test
	public void testGetKeyAsStreamContext(){
		
		keyStrategy.updateContext(3L, executionContext);
		
		assertEquals(1, executionContext.size());
		assertEquals(3L, executionContext.get(ClassUtils.getShortName(SingleColumnJdbcKeyCollector.class) + ".key"));
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
	
	@Transactional @Test
	public void testRestoreKeysFromNull(){
		
		try{
			keyStrategy.updateContext(null, null);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}
