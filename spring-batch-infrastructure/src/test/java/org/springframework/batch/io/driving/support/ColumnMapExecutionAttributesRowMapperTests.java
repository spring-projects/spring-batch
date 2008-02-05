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
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.core.CollectionFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;

/**
 * @author Lucas Ward
 */
public class ColumnMapExecutionAttributesRowMapperTests extends TestCase {

	private static final String KEY = ColumnMapExecutionAttributesRowMapper.KEY_PREFIX;
	
	private ColumnMapExecutionAttributesRowMapper mapper;
	
	private Map key;
	
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		mapper = new ColumnMapExecutionAttributesRowMapper();
		
		key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
	}
	
	public void testCreateExecutionAttributesWithInvalidType() throws Exception {
		
		try{
			mapper.createExecutionAttributes(new Object());
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionAttributesWithNull(){
		
		try{
			mapper.createExecutionAttributes(null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateExecutionAttributes() throws Exception {
		ExecutionAttributes streamContext = mapper.createExecutionAttributes(key);
		Properties props = streamContext.getProperties();
		assertEquals("1", props.getProperty(KEY + "0"));
		assertEquals("2", props.getProperty(KEY + "1"));
	}
	
	public void testCreateExecutionAttributesFromEmptyKeys() throws Exception {
		
		ExecutionAttributes streamContext = mapper.createExecutionAttributes(new HashMap());
		assertEquals(0, streamContext.getProperties().size());
	}
	
	public void testCreateSetter() throws Exception {
		
		Properties props = new Properties();
		props.setProperty(KEY + "0", "1");
		props.setProperty(KEY + "1", "2");
		ExecutionAttributes streamContext = new ExecutionAttributes();
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
