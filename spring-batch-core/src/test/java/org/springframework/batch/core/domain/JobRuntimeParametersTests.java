/**
 * 
 */
package org.springframework.batch.core.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class JobRuntimeParametersTests extends TestCase {

	JobRuntimeParameters parameters;
	
	Map stringMap;
	
	Map longMap;
	
	Map dateMap;
	Date date1 = new Date(4321431242L);
	Date date2 = new Date(7809089900L);
	
	protected void setUp() throws Exception {
		super.setUp();
		
		parameters = getNewParameters();
	}
	
	private JobRuntimeParameters getNewParameters(){
		
		stringMap = new HashMap();
		stringMap.put("string.key1", "value1");
		stringMap.put("string.key2", "value2");
		
		longMap = new HashMap();
		longMap.put("long.key1", new Long(1));
		longMap.put("long.key2", new Long(2));
		
		dateMap = new HashMap();
		dateMap.put("date.key1", date1 );
		dateMap.put("date.key2", date2 );
		
		return new JobRuntimeParameters(stringMap, longMap, dateMap);
	}
	
	public void testBadLongConstructorException() throws Exception{
				
		Map badLongMap = new HashMap();
		badLongMap.put("key", "bad long");
		
		try{
			JobRuntimeParameters testParameters = new JobRuntimeParameters(stringMap, badLongMap, dateMap);
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testBadStringConstructorException() throws Exception{
		
		Map badMap = new HashMap();
		badMap.put("key", new Integer(2));
		
		try{
			JobRuntimeParameters testParameters = new JobRuntimeParameters(badMap, longMap, dateMap);
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testBadDateConstructorException() throws Exception{
		
		Map badMap = new HashMap();
		badMap.put("key", new java.sql.Date(System.currentTimeMillis()));
		
		try{
			JobRuntimeParameters testParameters = new JobRuntimeParameters(stringMap, longMap, badMap);
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testGetString(){
		
		assertEquals("value1", parameters.getString("string.key1"));
		assertEquals("value2", parameters.getString("string.key2"));
	}
	
	public void testGetLong(){
		
		assertEquals(new Long(1), parameters.getLong("long.key1"));
		assertEquals(new Long(2), parameters.getLong("long.key2"));
	}
	
	public void testGetDate(){
		
		assertEquals(date1, parameters.getDate("date.key1"));
		assertEquals(date2, parameters.getDate("date.key2"));
	}
	
	public void testEquals(){
		
		JobRuntimeParameters testParameters = getNewParameters();	
		assertTrue(testParameters.equals(parameters));
	}
	
	public void testToString(){
		
		assertEquals("{string.key1=value1, string.key2=value2}{long.key1=1, long.key2=2}" +
				"{date.key2=Wed Apr 01 03:11:29 CST 1970, date.key1=Thu Feb 19 18:23:51 CST 1970}", parameters.toString());
	}
}
