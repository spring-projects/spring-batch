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
import org.springframework.batch.stream.GenericStreamContext;
import org.springframework.batch.stream.StreamContext;
import org.springframework.core.CollectionFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.util.ClassUtils;

/**
 * @author Lucas Ward
 */
public class ColumnMapRestartDataRowMapperTests extends TestCase {

	private static final String KEY = ClassUtils.getQualifiedName(ColumnMapRestartDataRowMapper.class) + ".KEY.";
	
	ColumnMapRestartDataRowMapper mapper;
	
	Map key;
	
	MockControl psControl = MockControl.createControl(PreparedStatement.class);
	PreparedStatement ps;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		mapper = new ColumnMapRestartDataRowMapper();
		
		key = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(2);
		key.put("1", new Integer(1));
		key.put("2", new Integer(2));
	}
	
	public void testCreateRestartDataWithInvalidType() throws Exception {
		
		try{
			mapper.createRestartData(new Object());
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateRestartDataWithNull(){
		
		try{
			mapper.createRestartData(null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateRestartData() throws Exception {
		
		StreamContext streamContext = mapper.createRestartData(key);
		Properties props = streamContext.getProperties();
		assertEquals("1", props.getProperty(KEY + "0"));
		assertEquals("2", props.getProperty(KEY + "1"));
	}
	
	public void testCreateRestartDataFromEmptyKeys() throws Exception {
		
		StreamContext streamContext = mapper.createRestartData(new HashMap());
		assertEquals(0, streamContext.getProperties().size());
	}
	
	public void testCreateSetter() throws Exception {
		
		Properties props = new Properties();
		props.setProperty(KEY + "0", "1");
		props.setProperty(KEY + "1", "2");
		StreamContext streamContext = new GenericStreamContext(props);
		PreparedStatementSetter setter = mapper.createSetter(streamContext);
		ps = (PreparedStatement)psControl.getMock();
		
		ps.setString(1, "1");
		ps.setString(2, "2");
		psControl.replay();
		
		setter.setValues(ps);
		
		psControl.verify();
	}
	
}
