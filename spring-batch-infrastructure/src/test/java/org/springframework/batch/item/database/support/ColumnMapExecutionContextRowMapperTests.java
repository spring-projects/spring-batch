/**
 * 
 */
package org.springframework.batch.item.database.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 * @author Will Schipp
 */
public class ColumnMapExecutionContextRowMapperTests extends TestCase {

	private ColumnMapItemPreparedStatementSetter mapper;
	
	private Map<String, Object> key;
	
	private PreparedStatement ps;
		
    @Override
	protected void setUp() throws Exception {
		super.setUp();
	
		ps = mock(PreparedStatement.class);
		mapper = new ColumnMapItemPreparedStatementSetter();
		
		key = new LinkedHashMap<String, Object>(2);
		key.put("1", Integer.valueOf(1));
		key.put("2", Integer.valueOf(2));
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		mapper.setValues(new HashMap<String, Object>(), ps);
	}
	
	public void testCreateSetter() throws Exception {
		
		ps.setObject(1, Integer.valueOf(1));
		ps.setObject(2, Integer.valueOf(2));
		mapper.setValues(key, ps);	
	}
	
}
