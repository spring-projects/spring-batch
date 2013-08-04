package org.springframework.batch.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

/**
 * @author Lucas Ward
 * @author Michael Minella
 *
 */
public class JobParametersBuilderTests {

	JobParametersBuilder parametersBuilder = new JobParametersBuilder();

	Date date = new Date(System.currentTimeMillis());

	@Test
	public void testNonIdentifyingParameters() {
		parametersBuilder.addDate("SCHEDULE_DATE", date, false);
		parametersBuilder.addLong("LONG", new Long(1), false);
		parametersBuilder.addString("STRING", "string value", false);
		JobParameters parameters = parametersBuilder.toJobParameters();
		assertEquals(date, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(1L, parameters.getLong("LONG").longValue());
		assertEquals("string value", parameters.getString("STRING"));
		assertFalse(parameters.getParameters().get("SCHEDULE_DATE").isIdentifying());
		assertFalse(parameters.getParameters().get("LONG").isIdentifying());
		assertFalse(parameters.getParameters().get("STRING").isIdentifying());
	}

	@Test
	public void testToJobRuntimeParamters(){
		parametersBuilder.addDate("SCHEDULE_DATE", date);
		parametersBuilder.addLong("LONG", new Long(1));
		parametersBuilder.addString("STRING", "string value");
		JobParameters parameters = parametersBuilder.toJobParameters();
		assertEquals(date, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(1L, parameters.getLong("LONG").longValue());
		assertEquals("string value", parameters.getString("STRING"));
	}

	@Test
	public void testNullRuntimeParamters(){
		parametersBuilder.addDate("SCHEDULE_DATE", null);
		parametersBuilder.addLong("LONG", null);
		parametersBuilder.addString("STRING", null);
		JobParameters parameters = parametersBuilder.toJobParameters();
		assertEquals(null, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(0L, parameters.getLong("LONG").longValue());
		assertEquals(null, parameters.getString("STRING"));
	}

	@Test
	public void testCopy(){
		parametersBuilder.addString("STRING", "string value");
		parametersBuilder = new JobParametersBuilder(parametersBuilder.toJobParameters());
		Iterator<String> parameters = parametersBuilder.toJobParameters().getParameters().keySet().iterator();
		assertEquals("STRING", parameters.next());
	}

	@Test
	public void testOrderedTypes(){
		parametersBuilder.addDate("SCHEDULE_DATE", date);
		parametersBuilder.addLong("LONG", new Long(1));
		parametersBuilder.addString("STRING", "string value");
		Iterator<String> parameters = parametersBuilder.toJobParameters().getParameters().keySet().iterator();
		assertEquals("SCHEDULE_DATE", parameters.next());
		assertEquals("LONG", parameters.next());
		assertEquals("STRING", parameters.next());
	}

	@Test
	public void testOrderedStrings(){
		parametersBuilder.addString("foo", "value foo");
		parametersBuilder.addString("bar", "value bar");
		parametersBuilder.addString("spam", "value spam");
		Iterator<String> parameters = parametersBuilder.toJobParameters().getParameters().keySet().iterator();
		assertEquals("foo", parameters.next());
		assertEquals("bar", parameters.next());
		assertEquals("spam", parameters.next());
	}

	@Test
	public void testAddJobParameter(){
		JobParameter jobParameter = new JobParameter("bar");
		parametersBuilder.addParameter("foo", jobParameter);
		Map<String,JobParameter> parameters = parametersBuilder.toJobParameters().getParameters();
		assertEquals(1, parameters.size());
		assertEquals("bar", parameters.get("foo").getValue());
	}
}
