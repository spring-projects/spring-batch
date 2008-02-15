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

	private static final String KEY = ColumnMapExecutionContextRowMapper.KEY_PREFIX;
	
	private ColumnMapExecutionContextRowMapper mapper;
	
	private Map key;
	
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		mapper = new ColumnMapExecutionContextRowMapper();
		
		key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
	}
	
	public void testCreateExecutionContextWithInvalidType() throws Exception {
		
		try{
			mapper.createExecutionContext(new Object());
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContextWithNull(){
		
		try{
			mapper.createExecutionContext(null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionContext() throws Exception {
		ExecutionContext streamContext = mapper.createExecutionContext(key);
		Properties props = streamContext.getProperties();
		assertEquals("1", props.getProperty(KEY + "0"));
		assertEquals("2", props.getProperty(KEY + "1"));
	}
	
	public void testCreateExecutionContextFromEmptyKeys() throws Exception {
		
		ExecutionContext streamContext = mapper.createExecutionContext(new HashMap());
		assertEquals(0, streamContext.getProperties().size());
	}
	
	public void testCreateSetter() throws Exception {
		
		Properties props = new Properties();
		props.setProperty(KEY + "0", "1");
		props.setProperty(KEY + "1", "2");
		ExecutionContext streamContext = new ExecutionContext();
		streamContext.putString(KEY + "0", "1");
		streamContext.putString(KEY + "1", "2");
		PreparedStatementSetter setter = mapper.createSetter(streamContext);
		ps = (PreparedStatement)psControl.getMock();
		
		ps.setString(1, "1");
		ps.setString(2, "2");
		psControl.replay();
		
		setter.setValues(ps);
		
		psControl.verify();
	}
	
}
