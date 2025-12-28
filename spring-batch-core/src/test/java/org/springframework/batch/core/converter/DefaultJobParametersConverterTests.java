/*
 * Copyright 2006-present the original author or authors.
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
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
		assertTrue(props.getParameter("job.key").identifying());
		assertTrue(props.getParameter("schedule.date").identifying());
		assertTrue(props.getParameter("vendor.id").identifying());
	}

	@Test
	void testGetParametersIdentifyingByDefault() {
		String jobKey = "job.key=myKey,java.lang.String";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date";
		String vendorId = "vendor.id=33243243,java.lang.Long";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameter("job.key").identifying());
		assertTrue(props.getParameter("schedule.date").identifying());
		assertTrue(props.getParameter("vendor.id").identifying());
	}

	@Test
	void testGetParametersNonIdentifying() {
		String jobKey = "job.key=myKey,java.lang.String,false";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date,false";
		String vendorId = "vendor.id=33243243,java.lang.Long,false";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertFalse(props.getParameter("job.key").identifying());
		assertFalse(props.getParameter("schedule.date").identifying());
		assertFalse(props.getParameter("vendor.id").identifying());
	}

	@Test
	void testGetParametersMixed() {
		String jobKey = "job.key=myKey,java.lang.String,true";
		String scheduleDate = "schedule.date=2008-01-23T10:15:30Z,java.util.Date";
		String vendorId = "vendor.id=33243243,java.lang.Long,false";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameter("job.key").identifying());
		assertTrue(props.getParameter("schedule.date").identifying());
		assertFalse(props.getParameter("vendor.id").identifying());
	}

	@Test
	void testGetParameters() {
		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date=2008-01-23,java.time.LocalDate,true";
		String vendorId = "vendor.id=33243243,java.lang.Long,true";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals(33243243L, props.getLong("vendor.id").longValue());
		LocalDate expectedDate = LocalDate.of(2008, 1, 23);
		assertEquals(expectedDate, props.getParameter("schedule.date").value());
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
		assertEquals(1, jobParameters.parameters().size());
		JobParameter<?> parameter = jobParameters.getParameter("parameter");
		assertEquals("parameter", parameter.name());
		assertEquals("", parameter.value());
		assertEquals(String.class, parameter.type());
		assertTrue(parameter.identifying());
	}

	@Test
	void testGetParametersWithDoubleValueDeclaredAsLong() {

		String[] args = new String[] { "value=1.03,java.lang.Long" };

		assertThatExceptionOfType(JobParametersConversionException.class)
			.isThrownBy(() -> factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "=")))
			.withMessageContaining("1.03");
	}

	@Test
	void testGetParametersWithBogusDouble() {

		String[] args = new String[] { "value=foo,java.lang.Double" };

		assertThatExceptionOfType(JobParametersConversionException.class)
			.isThrownBy(() -> factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "=")))
			.withMessageContaining("foo");
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
	void testGetParametersWithZonedDateTime() {
		// given
		String[] args = new String[] { "parameter=2023-12-25T10:30:00+01:00[Europe/Paris],java.time.ZonedDateTime" };

		// when
		JobParameters jobParameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		// then
		assertEquals(1, jobParameters.parameters().size());
		JobParameter<?> parameter = jobParameters.getParameter("parameter");
		assertEquals("parameter", parameter.name());
		assertEquals(ZonedDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneId.of("Europe/Paris")), parameter.value());
		assertEquals(ZonedDateTime.class, parameter.type());
		assertTrue(parameter.identifying());
	}

	@Test
	void testGetParametersWithOffsetDateTime() {
		// given
		String[] args = new String[] { "parameter=2023-12-25T10:30:00+01:00,java.time.OffsetDateTime" };

		// when
		JobParameters jobParameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		// then
		assertEquals(1, jobParameters.parameters().size());
		JobParameter<?> parameter = jobParameters.getParameter("parameter");
		assertEquals("parameter", parameter.name());
		assertEquals(OffsetDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneOffset.of("+01:00")), parameter.value());
		assertEquals(OffsetDateTime.class, parameter.type());
		assertTrue(parameter.identifying());
	}

	@Test
	void testGetProperties() {
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
		assertTrue(props.parameters().isEmpty());
	}

	@Test
	void testGetPropertiesWithZonedDateTime() {
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"));
		JobParameters parameters = new JobParametersBuilder()
			.addJobParameter("schedule.zonedDateTime", zonedDateTime, ZonedDateTime.class, true)
			.toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("2023-12-25T10:30:00+09:00[Asia/Seoul],java.time.ZonedDateTime,true",
				props.getProperty("schedule.zonedDateTime"));
	}

	@Test
	void testGetPropertiesWithOffsetDateTime() {
		OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneOffset.of("+09:00"));
		JobParameters parameters = new JobParametersBuilder()
			.addJobParameter("schedule.offsetDateTime", offsetDateTime, OffsetDateTime.class, true)
			.toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("2023-12-25T10:30:00+09:00,java.time.OffsetDateTime,true",
				props.getProperty("schedule.offsetDateTime"));
	}

	@Test
	void testRoundTripWithZonedDateTime() {
		String[] args = new String[] {
				"schedule.zonedDateTime=2023-12-25T10:30:00+09:00[Asia/Seoul],java.time.ZonedDateTime" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("2023-12-25T10:30:00+09:00[Asia/Seoul],java.time.ZonedDateTime,true",
				props.getProperty("schedule.zonedDateTime"));
	}

	@Test
	void testRoundTripWithOffsetDateTime() {
		String[] args = new String[] { "schedule.offsetDateTime=2023-12-25T10:30:00+09:00,java.time.OffsetDateTime" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("2023-12-25T10:30:00+09:00,java.time.OffsetDateTime,true",
				props.getProperty("schedule.offsetDateTime"));
	}

}
