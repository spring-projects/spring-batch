/**
 * 
 */
package org.springframework.batch.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.commons.lang.SerializationUtils;

/**
 * @author Lucas Ward
 * 
 */
public class JobParametersTests extends TestCase {

	JobParameters parameters;

	Date date1 = new Date(4321431242L);

	Date date2 = new Date(7809089900L);

	protected void setUp() throws Exception {
		super.setUp();
		parameters = getNewParameters();
	}

	private JobParameters getNewParameters() {

		Map<String, JobParameter> parameterMap = new HashMap<String, JobParameter>();
		parameterMap.put("string.key1", new JobParameter("value1"));
		parameterMap.put("string.key2", new JobParameter("value2"));
		parameterMap.put("long.key1", new JobParameter(1L));
		parameterMap.put("long.key2", new JobParameter(2L));
		parameterMap.put("double.key1", new JobParameter(1.1));
		parameterMap.put("double.key2", new JobParameter(2.2));
		parameterMap.put("date.key1", new JobParameter(date1));
		parameterMap.put("date.key2", new JobParameter(date2));

		return new JobParameters(parameterMap);
	}


	public void testGetString() {
		assertEquals("value1", parameters.getString("string.key1"));
		assertEquals("value2", parameters.getString("string.key2"));
	}

	public void testGetLong() {
		assertEquals(1L, parameters.getLong("long.key1"));
		assertEquals(2L, parameters.getLong("long.key2"));
	}

	public void testGetDouble() {
		assertEquals(new Double(1.1), parameters.getDouble("double.key1"));
		assertEquals(new Double(2.2), parameters.getDouble("double.key2"));
	}

	public void testGetDate() {
		assertEquals(date1, parameters.getDate("date.key1"));
		assertEquals(date2, parameters.getDate("date.key2"));
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

		Map<String, JobParameter> props = parameters.getParameters();
		StringBuffer stringBuilder = new StringBuffer();
		for (Entry<String, JobParameter> entry : props.entrySet()) {
			stringBuilder.append(entry.toString() + ";");
		}

		String string1 = stringBuilder.toString();

		Map<String, JobParameter> parameterMap = new HashMap<String, JobParameter>();
		parameterMap.put("string.key2", new JobParameter("value2"));
		parameterMap.put("string.key1", new JobParameter("value1"));
		parameterMap.put("long.key2", new JobParameter(2L));
		parameterMap.put("long.key1", new JobParameter(1L));
		parameterMap.put("double.key2", new JobParameter(2.2));
		parameterMap.put("double.key1", new JobParameter(1.1));
		parameterMap.put("date.key2", new JobParameter(date2));
		parameterMap.put("date.key1", new JobParameter(date1));
		
		JobParameters testProps = new JobParameters(parameterMap);

		props = testProps.getParameters();
		stringBuilder = new StringBuffer();
		for (Entry<String, JobParameter> entry : props.entrySet()) {
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

		byte[] serialized =
		SerializationUtils.serialize(params);

		assertEquals(params, SerializationUtils.deserialize(serialized));
	}
}
