/**
 * 
 */
package org.springframework.batch.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.support.SerializationUtils;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class JobParametersTests {

	JobParameters parameters;

	Date date1 = new Date(4321431242L);

	Date date2 = new Date(7809089900L);

	@Before
	public void setUp() throws Exception {
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


	@Test
	public void testGetString() {
		assertEquals("value1", parameters.getString("string.key1"));
		assertEquals("value2", parameters.getString("string.key2"));
	}

	@Test
	public void testGetNullString() {
		parameters = new JobParameters(Collections.singletonMap("string.key1", new JobParameter((String)null)));
		assertEquals(null, parameters.getDate("string.key1"));
	}

	@Test
	public void testGetLong() {
		assertEquals(1L, parameters.getLong("long.key1"));
		assertEquals(2L, parameters.getLong("long.key2"));
	}

	@Test
	public void testGetDouble() {
		assertEquals(new Double(1.1), new Double(parameters.getDouble("double.key1")));
		assertEquals(new Double(2.2), new Double(parameters.getDouble("double.key2")));
	}

	@Test
	public void testGetDate() {
		assertEquals(date1, parameters.getDate("date.key1"));
		assertEquals(date2, parameters.getDate("date.key2"));
	}

	@Test
	public void testGetNullDate() {
		parameters = new JobParameters(Collections.singletonMap("date.key1", new JobParameter((Date)null)));
		assertEquals(null, parameters.getDate("date.key1"));
	}

	@Test
	public void testGetEmptyLong() {
		parameters = new JobParameters(Collections.singletonMap("long1", new JobParameter((Long)null)));
		assertEquals(0L, parameters.getLong("long1"));
	}

	@Test
	public void testGetMissingLong() {
		assertEquals(0L, parameters.getLong("missing.long1"));
	}

	@Test
	public void testGetMissingDouble() {
		assertEquals(0.0, parameters.getDouble("missing.double1"), 0.0001);
	}

	@Test
	public void testIsEmptyWhenEmpty() throws Exception {
		assertTrue(new JobParameters().isEmpty());
	}

	@Test
	public void testIsEmptyWhenNotEmpty() throws Exception {
		assertFalse(parameters.isEmpty());
	}

	@Test
	public void testEquals() {
		JobParameters testParameters = getNewParameters();
		assertTrue(testParameters.equals(parameters));
	}

	@Test
	public void testEqualsSelf() {
		assertTrue(parameters.equals(parameters));
	}

	@Test
	public void testEqualsDifferent() {
		assertFalse(parameters.equals(new JobParameters()));
	}

	@Test
	public void testEqualsWrongType() {
		assertFalse(parameters.equals("foo"));
	}

	@Test
	public void testEqualsNull() {
		assertFalse(parameters.equals(null));
	}

	@Test
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

	@Test
	public void testHashCodeEqualWhenEmpty() throws Exception {
		int code = new JobParameters().hashCode();
		assertEquals(code, new JobParameters().hashCode());
	}

	@Test
	public void testHashCodeEqualWhenNotEmpty() throws Exception {
		int code = getNewParameters().hashCode();
		assertEquals(code, parameters.hashCode());
	}

	@Test
	public void testSerialization() {
		JobParameters params = getNewParameters();

		byte[] serialized =
		SerializationUtils.serialize(params);

		assertEquals(params, SerializationUtils.deserialize(serialized));
	}
    
    @Test
    public void testLongReturns0WhenKeyDoesntExit(){
        assertEquals(0L,new JobParameters().getLong("keythatdoesntexist"));
    }

    @Test
    public void testStringReturnsNullWhenKeyDoesntExit(){
        assertNull(new JobParameters().getString("keythatdoesntexist"));
    }

    @Test
    public void testDoubleReturns0WhenKeyDoesntExit(){
        assertEquals(0.0,new JobParameters().getLong("keythatdoesntexist"), 0.0001);
    }

    @Test
    public void testDateReturnsNullWhenKeyDoesntExit(){
        assertNull(new JobParameters().getDate("keythatdoesntexist"));
    }
}
