/**
 * 
 */
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.core.CollectionFactory;

/**
 * @author Lucas Ward
 */
public class ColumnMapExecutionContextRowMapperTests extends TestCase {

	private ColumnMapKeyMappingPreparedStatementSetter mapper;
	
	private Map key;
	
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps;
		
	protected void setUp() throws Exception {
		super.setUp();
	
		ps = (PreparedStatement)psControl.getMock();
		mapper = new ColumnMapKeyMappingPreparedStatementSetter();
		
		key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
	}
	
	public void testSetValuesWithInvalidType() throws Exception {
		
		try{
			mapper.setValues(ps, new Object());
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContextWithNull() throws Exception{
		
		try{
			mapper.setValues(ps, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		psControl.replay();
		mapper.setValues(ps, new HashMap());
		psControl.verify();
	}
	
	public void testCreateSetter() throws Exception {
		
		ps.setObject(1, new Integer(1));
		ps.setObject(2, new Integer(2));
		psControl.replay();
		mapper.setValues(ps, key);	
		psControl.verify();
	}
	
}
