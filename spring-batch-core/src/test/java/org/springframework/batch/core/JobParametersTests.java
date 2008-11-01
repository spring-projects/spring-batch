/**
 * 
 */
package org.springframework.batch.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.SerializationUtils;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 * 
 */
public class JobParametersTests extends TestCase {

	JobParameters parameters;

	Map stringMap;

	Map longMap;

	Map dateMap;
	
	Map doubleMap;

	Date date1 = new Date(4321431242L);

	Date date2 = new Date(7809089900L);

	protected void setUp() throws Exception {
		super.setUp();
		parameters = getNewParameters();
	}

	private JobParameters getNewParameters() {

		stringMap = new HashMap();
		stringMap.put("string.key1", "value1");
		stringMap.put("string.key2", "value2");

		longMap = new HashMap();
		longMap.put("long.key1", new Long(1));
		longMap.put("long.key2", new Long(2));
		
		doubleMap = new HashMap();
		doubleMap.put("double.key1", new Double(1.1));
		doubleMap.put("double.key2", new Double(2.2));

		dateMap = new HashMap();
		dateMap.put("date.key1", date1);
		dateMap.put("date.key2", date2);

		return new JobParameters(stringMap, longMap, doubleMap, dateMap);
	}

	public void testBadLongKeyException() throws Exception {

		Map badLongMap = new HashMap();
		badLongMap.put(new Long(0), new Long(1));

		try {
			new JobParameters(stringMap, badLongMap, doubleMap, dateMap);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testBadLongConstructorException() throws Exception {

		Map badLongMap = new HashMap();
		badLongMap.put("key", "bad long");

		try {
			new JobParameters(stringMap, badLongMap, doubleMap, dateMap);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}
	
	public void testBadDoubleConstructorException() throws Exception {

		Map badDoubleMap = new HashMap();
		badDoubleMap.put("key", "bad double");

		try {
			new JobParameters(stringMap, longMap, badDoubleMap, dateMap);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testBadStringConstructorException() throws Exception {

		Map badMap = new HashMap();
		badMap.put("key", new Integer(2));

		try {
			new JobParameters(badMap, longMap, doubleMap, dateMap);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testBadDateConstructorException() throws Exception {

		Map badMap = new HashMap();
		badMap.put("key", new java.sql.Date(System.currentTimeMillis()));

		try {
			new JobParameters(stringMap, longMap, doubleMap, badMap);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testGetString() {
		assertEquals("value1", parameters.getString("string.key1"));
		assertEquals("value2", parameters.getString("string.key2"));
	}

	public void testGetStringParameters() {
		assertEquals("value1", parameters.getStringParameters().get("string.key1"));
		assertEquals("value2", parameters.getStringParameters().get("string.key2"));
	}

	public void testGetLong() {
		assertEquals(new Long(1), parameters.getLong("long.key1"));
		assertEquals(new Long(2), parameters.getLong("long.key2"));
	}

	public void testGetLongParameters() {
		assertEquals(new Long(1), parameters.getLongParameters().get("long.key1"));
		assertEquals(new Long(2), parameters.getLongParameters().get("long.key2"));
	}
	
	public void testGetDouble() {
		assertEquals(new Double(1.1), parameters.getDouble("double.key1"));
		assertEquals(new Double(2.2), parameters.getDouble("double.key2"));
	}
	
	public void testGetDoubleParameters() {
		assertEquals(new Double(1.1), parameters.getDoubleParameters().get("double.key1"));
		assertEquals(new Double(2.2), parameters.getDoubleParameters().get("double.key2"));
	}

	public void testGetDate() {
		assertEquals(date1, parameters.getDate("date.key1"));
		assertEquals(date2, parameters.getDate("date.key2"));
	}

	public void testGetDateParameters() {
		assertEquals(date1, parameters.getDateParameters().get("date.key1"));
		assertEquals(date2, parameters.getDateParameters().get("date.key2"));
	}

	public void testIsEmptyWhenEmpty() throws Exception {
		assertTrue(new JobParameters().isEmpty());
	}

	public void testIsEmptyWhenNotEmpty() throws Exception {
		assertFalse(parameters.isEmpty());
	}

	public void testEquals() {
		JobParameters testParameters = getNewParameters();
		assertTrue(testParameters.equals(parameters));
	}

	public void testEqualsSelf() {
		assertTrue(parameters.equals(parameters));
	}

	public void testEqualsDifferent() {
		assertFalse(parameters.equals(new JobParameters()));
	}

	public void testEqualsWrongType() {
		assertFalse(parameters.equals("foo"));
	}

	public void testEqualsNull() {
		assertFalse(parameters.equals(null));
	}

	public void testToStringOrder() {

		Map props = parameters.getParameters();
		StringBuffer stringBuilder = new StringBuffer();
		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			stringBuilder.append(entry.toString() + ";");
		}

		String string1 = stringBuilder.toString();

		stringMap = new HashMap();
		stringMap.put("string.key2", "value2");
		stringMap.put("string.key1", "value1");

		longMap = new HashMap();
		longMap.put("long.key2", new Long(2));
		longMap.put("long.key1", new Long(1));
		
		doubleMap = new HashMap();
		doubleMap.put("double.key2", new Double(2.2));
		doubleMap.put("double.key1", new Double(1.1));

		dateMap = new HashMap();
		dateMap.put("date.key2", date2);
		dateMap.put("date.key1", date1);

		JobParameters testProps = new JobParameters(stringMap, longMap, doubleMap, dateMap);

		props = testProps.getParameters();
		stringBuilder = new StringBuffer();
		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			stringBuilder.append(entry.toString() + ";");
		}
		String string2 = stringBuilder.toString();

		assertEquals(string1, string2);
	}

	public void testHashCodeEqualWhenEmpty() throws Exception {
		int code = new JobParameters().hashCode();
		assertEquals(code, new JobParameters().hashCode());
	}

	public void testHashCodeEqualWhenNotEmpty() throws Exception {
		int code = getNewParameters().hashCode();
		assertEquals(code, parameters.hashCode());
	}
	
	public void testSerialization() {
		JobParameters params = getNewParameters();
		
		byte[] serialized = SerializationUtils.serialize(params);
		
		assertEquals(params, SerializationUtils.deserialize(serialized));
	}
}
