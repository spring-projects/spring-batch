/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Michael Minella
 *
 */
class DefaultJobParametersConverterTests {

	private final DefaultJobParametersConverter factory = new DefaultJobParametersConverter();

	private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	@Test
	void testGetParametersIdentifyingWithIdentifyingKey() {
		String jobKey = "+job.key=myKey";
		String scheduleDate = "+schedule.date(date)=2008/01/23";
		String vendorId = "+vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameters().get("job.key").isIdentifying());
		assertTrue(props.getParameters().get("schedule.date").isIdentifying());
		assertTrue(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersIdentifyingByDefault() {
		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date(date)=2008/01/23";
		String vendorId = "vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertTrue(props.getParameters().get("job.key").isIdentifying());
		assertTrue(props.getParameters().get("schedule.date").isIdentifying());
		assertTrue(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersNonIdentifying() {
		String jobKey = "-job.key=myKey";
		String scheduleDate = "-schedule.date(date)=2008/01/23";
		String vendorId = "-vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertFalse(props.getParameters().get("job.key").isIdentifying());
		assertFalse(props.getParameters().get("schedule.date").isIdentifying());
		assertFalse(props.getParameters().get("vendor.id").isIdentifying());
	}

	@Test
	void testGetParametersMixed() {
		String jobKey = "+job.key=myKey";
		String scheduleDate = "schedule.date(date)=2008/01/23";
		String vendorId = "-vendor.id(long)=33243243";

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
		String scheduleDate = "schedule.date(date)=2008/01/23";
		String vendorId = "vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals(33243243L, props.getLong("vendor.id").longValue());
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	@Test
	void testGetParametersWithDateFormat() throws Exception {

		String[] args = new String[] { "schedule.date(date)=2008/23/01" };

		factory.setDateFormat(new SimpleDateFormat("yyyy/dd/MM"));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	@Test
	void testGetParametersWithBogusDate() {

		String[] args = new String[] { "schedule.date(date)=20080123" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("20080123"), "Message should contain wrong date: " + message);
			assertTrue(message.contains("yyyy/MM/dd"), "Message should contain format: " + message);
		}
	}

	@Test
	void testGetParametersWithNumberFormat() {

		String[] args = new String[] { "value(long)=1,000" };

		factory.setNumberFormat(new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1000L, props.getLong("value").longValue());
	}

	@Test
	void testGetParametersWithBogusLong() {

		String[] args = new String[] { "value(long)=foo" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("foo"), "Message should contain wrong number: " + message);
			assertTrue(message.contains("#"), "Message should contain format: " + message);
		}
	}

	@Test
	void testGetParametersWithDoubleValueDeclaredAsLong() {

		String[] args = new String[] { "value(long)=1.03" };
		factory.setNumberFormat(new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("1.03"), "Message should contain wrong number: " + message);
			assertTrue(message.contains("decimal"), "Message should contain 'decimal': " + message);
		}
	}

	@Test
	void testGetParametersWithBogusDouble() {

		String[] args = new String[] { "value(double)=foo" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("foo"), "Message should contain wrong number: " + message);
			assertTrue(message.contains("#"), "Message should contain format: " + message);
		}
	}

	@Test
	void testGetParametersWithDouble() {

		String[] args = new String[] { "value(double)=1.38" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.38, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetParametersWithDoubleAndLongAndNumberFormat() {

		String[] args = new String[] { "value(double)=1,23456", "long(long)=123.456" };
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		factory.setNumberFormat(format);

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.23456, props.getDouble("value"), Double.MIN_VALUE);
		assertEquals(123456, props.getLong("long").longValue());

	}

	@Test
	void testGetParametersWithRoundDouble() {

		String[] args = new String[] { "value(double)=1.0" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetParametersWithVeryRoundDouble() {

		String[] args = new String[] { "value(double)=1" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	void testGetProperties() throws Exception {

		JobParameters parameters = new JobParametersBuilder().addDate("schedule.date", dateFormat.parse("01/23/2008"))
				.addString("job.key", "myKey").addLong("vendor.id", 33243243L).addDouble("double.key", 1.23)
				.toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
		assertEquals("1.23", props.getProperty("double.key(double)"));
	}

	@Test
	void testRoundTrip() {

		String[] args = new String[] { "schedule.date(date)=2008/01/23", "job.key=myKey", "vendor.id(long)=33243243",
				"double.key(double)=1.23" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
		assertEquals("1.23", props.getProperty("double.key(double)"));
	}

	@Test
	void testRoundTripWithIdentifyingAndNonIdentifying() {

		String[] args = new String[] { "schedule.date(date)=2008/01/23", "+job.key=myKey", "-vendor.id(long)=33243243",
				"double.key(double)=1.23" };

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("-vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
		assertEquals("1.23", props.getProperty("double.key(double)"));
	}

	@Test
	void testRoundTripWithNumberFormat() {

		String[] args = new String[] { "schedule.date(date)=2008/01/23", "job.key=myKey", "vendor.id(long)=33243243",
				"double.key(double)=1,23" };
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		factory.setNumberFormat(format);

		JobParameters parameters = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
		assertEquals("1,23", props.getProperty("double.key(double)"));
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
