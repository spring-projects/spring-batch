/**
 * 
 */
package org.springframework.batch.io.driving.support;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.CollectionFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;

/**
 * @author Lucas Ward
 */
public class ColumnMapExecutionContextRowMapperTests extends TestCase {

	private ColumnMapExecutionContextRowMapper mapper;
	
	private Map key;
	
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps;
	
	private ExecutionContext executionContext;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		mapper = new ColumnMapExecutionContextRowMapper();
		
		key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
		
		executionContext = new ExecutionContext();
	}
	
	public void testCreateExecutionContextWithInvalidType() throws Exception {
		
		try{
			mapper.mapKeys(new Object(), executionContext);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContextWithNull(){
		
		try{
			mapper.mapKeys(null, null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContext() throws Exception {
		mapper.mapKeys(key, executionContext);
		Properties props = executionContext.getProperties();
		assertEquals("1", props.getProperty("1"));
		assertEquals("2", props.getProperty("2"));
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		mapper.mapKeys(new HashMap(), executionContext);
		assertEquals(0, executionContext.size());
	}
	
	public void testCreateSetter() throws Exception {
		
		ExecutionContext streamContext = new ExecutionContext();
		streamContext.putString("0", "1");
		streamContext.putString("1", "2");
		PreparedStatementSetter setter = mapper.createSetter(streamContext);
		ps = (PreparedStatement)psControl.getMock();
		
		ps.setString(1, "2");
		ps.setString(2, "1");
		psControl.replay();
		
		setter.setValues(ps);
		
		psControl.verify();
	}
	
}
