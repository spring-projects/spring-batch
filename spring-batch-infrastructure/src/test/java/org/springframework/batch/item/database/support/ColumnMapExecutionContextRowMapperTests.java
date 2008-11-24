/**
 * 
 */
package org.springframework.batch.item.database.support;

import static org.easymock.EasyMock.*;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 */
public class ColumnMapExecutionContextRowMapperTests extends TestCase {

	private ColumnMapItemPreparedStatementSetter mapper;
	
	private Map<String, Object> key;
	
	private PreparedStatement ps;
		
	protected void setUp() throws Exception {
		super.setUp();
	
		ps = createMock(PreparedStatement.class);
		mapper = new ColumnMapItemPreparedStatementSetter();
		
		key = new LinkedHashMap<String, Object>(2);
		key.put("1", Integer.valueOf(1));
		key.put("2", Integer.valueOf(2));
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		replay(ps);
		mapper.setValues(new HashMap<String, Object>(), ps);
		verify(ps);
	}
	
	public void testCreateSetter() throws Exception {
		
		ps.setObject(1, Integer.valueOf(1));
		ps.setObject(2, Integer.valueOf(2));
		replay(ps);
		mapper.setValues(key, ps);	
		verify(ps);
	}
	
}
