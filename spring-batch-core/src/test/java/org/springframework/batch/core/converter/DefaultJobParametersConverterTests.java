/*
 * Copyright 2006-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.converter;

import java.time.LocalDate;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
class DefaultJobParametersConverterTests {

	private final DefaultJobParametersConverter factory = new DefaultJobParametersConverter();

	@Test
	void testGetParametersIdentifyingWithIdentifyingKey() {
		String jobKey = "job.key=myKey,java.lang.String,true";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date,true";
		String vendorId = "vendor.id=33243243,java.lang.Long,true";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameters().get("job.key").isIdentifying());
		assertTrue(props.getParameters().get("schedule.date").isIdentifying());
		assertTrue(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersIdentifyingByDefault() {
		String jobKey = "job.key=myKey,java.lang.String";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date";
		String vendorId = "vendor.id=33243243,java.lang.Long";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameters().get("job.key").isIdentifying());
		assertTrue(props.getParameters().get("schedule.date").isIdentifying());
		assertTrue(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersNonIdentifying() {
		String jobKey = "job.key=myKey,java.lang.String,false";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date,false";
		String vendorId = "vendor.id=33243243,java.lang.Long,false";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertFalse(props.getParameters().get("job.key").isIdentifying());
		assertFalse(props.getParameters().get("schedule.date").isIdentifying());
		assertFalse(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersMixed() {
		String jobKey = "job.key=myKey,java.lang.String,true";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date";
		String vendorId = "vendor.id=33243243,java.lang.Long,false";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameters().get("job.key").isIdentifying());
		assertTrue(props.getParameters().get("schedule.date").isIdentifying());
		assertFalse(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParameters() throws Exception {
		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date=2008-01-23,java.time.LocalDate,true";
		String vendorId = "vendor.id=33243243,java.lang.Long,true";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals(33243243L, props.getLong("vendor.id").longValue());
		LocalDate expectedDate = LocalDate.of(2008, 1, 23);
		assertEquals(expectedDate, props.getParameter("schedule.date").getValue());
	}

	@Test
	void testGetParametersWithBogusLong() {

		String[] args = new String[] { "value=foo,java.lang.Long" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (JobParametersConversionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("foo"), "Message should contain wrong number: " + message);
		}
	}

	@Test
	void testGetParametersWithEmptyValue() {
		// given
		String[] args = new String[] { "parameter=" };

		// when
		JobParameters jobParameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		// then
		assertEquals(1, jobParameters.getParameters().size());
		JobParameter<?> parameter = jobParameters.getParameters().get("parameter");
		assertEquals("", parameter.getValue());
		assertEquals(String.class, parameter.getType());
		assertTrue(parameter.isIdentifying());
	}

	@Test
	void testGetParametersWithDoubleValueDeclaredAsLong() {

		String[] args = new String[] { "value=1.03,java.lang.Long" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (JobParametersConversionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("1.03"), "Message should contain wrong number: " + message);
		}
	}

	@Test
	void testGetParametersWithBogusDouble() {

		String[] args = new String[] { "value=foo,java.lang.Double" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (JobParametersConversionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("foo"), "Message should contain wrong number: " + message);
		}
	}

	@Test
	void testGetParametersWithDouble() {

		String[] args = new String[] { "value=1.38,java.lang.Double" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.38, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetParametersWithRoundDouble() {

		String[] args = new String[] { "value=1.0,java.lang.Double" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetParametersWithVeryRoundDouble() {

		String[] args = new String[] { "value=1,java.lang.Double" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetProperties() throws Exception {
		LocalDate date = LocalDate.of(2008, 1, 23);
		JobParameters parameters = new JobParametersBuilder()
			.addJobParameter("schedule.date", date, LocalDate.class, true)
			.addString("job.key", "myKey")
			.addLong("vendor.id", 33243243L)
			.addDouble("double.key", 1.23)
			.toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey,java.lang.String,true", props.getProperty("job.key"));
		assertEquals("33243243,java.lang.Long,true", props.getProperty("vendor.id"));
		assertEquals("2008-01-23,java.time.LocalDate,true", props.getProperty("schedule.date"));
		assertEquals("1.23,java.lang.Double,true", props.getProperty("double.key"));
	}

	@Test
	void testRoundTrip() {

		String[] args = new String[] { "schedule.date=2008-01-23,java.time.LocalDate", "job.key=myKey",
				"vendor.id=33243243,java.lang.Long", "double.key=1.23,java.lang.Double" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey,java.lang.String,true", props.getProperty("job.key"));
		assertEquals("33243243,java.lang.Long,true", props.getProperty("vendor.id"));
		assertEquals("2008-01-23,java.time.LocalDate,true", props.getProperty("schedule.date"));
		assertEquals("1.23,java.lang.Double,true", props.getProperty("double.key"));
	}

	@Test
	void testRoundTripWithIdentifyingAndNonIdentifying() {

		String[] args = new String[] { "schedule.date=2008-01-23,java.time.LocalDate", "job.key=myKey",
				"vendor.id=33243243,java.lang.Long,false", "double.key=1.23,java.lang.Double" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey,java.lang.String,true", props.getProperty("job.key"));
		assertEquals("33243243,java.lang.Long,false", props.getProperty("vendor.id"));
		assertEquals("2008-01-23,java.time.LocalDate,true", props.getProperty("schedule.date"));
		assertEquals("1.23,java.lang.Double,true", props.getProperty("double.key"));
	}

	@Test
	void testEmptyArgs() {
		JobParameters props = factory.getJobParameters(new Properties());
		assertTrue(props.getParameters().isEmpty());
	}

	@Test
	void testNullArgs() {
		assertEquals(new JobParameters(), factory.getJobParameters(null));
		assertEquals(new Properties(), factory.getProperties(null));
	}

}
