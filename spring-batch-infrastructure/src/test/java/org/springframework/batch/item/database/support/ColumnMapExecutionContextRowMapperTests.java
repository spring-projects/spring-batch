/**
 * 
 */
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.MockControl;

/**
 * @author Lucas Ward
 */
public class ColumnMapExecutionContextRowMapperTests extends TestCase {

	private ColumnMapItemPreparedStatementSetter mapper;
	
	private Map<String, Object> key;
	
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps;
		
	@SuppressWarnings("unchecked")
	protected void setUp() throws Exception {
		super.setUp();
	
		ps = (PreparedStatement)psControl.getMock();
		mapper = new ColumnMapItemPreparedStatementSetter();
		
		key = new LinkedHashMap<String, Object>(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		psControl.replay();
		mapper.setValues(new HashMap<String, Object>(), ps);
		psControl.verify();
	}
	
	public void testCreateSetter() throws Exception {
		
		ps.setObject(1, new Integer(1));
		ps.setObject(2, new Integer(2));
		psControl.replay();
		mapper.setValues(key, ps);	
		psControl.verify();
	}
	
}
