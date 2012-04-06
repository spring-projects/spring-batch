/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 
 */
public class DefaultJobParametersConverterTests {

	DefaultJobParametersConverter factory = new DefaultJobParametersConverter();

	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	@Test
	public void testGetParameters() throws Exception {

		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date(date)=2008/01/23";
		String vendorId = "vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals(33243243L, props.getLong("vendor.id"));
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	@Test
	public void testGetParametersWithDateFormat() throws Exception {

		String[] args = new String[] { "schedule.date(date)=2008/23/01" };

		factory.setDateFormat(new SimpleDateFormat("yyyy/dd/MM"));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	@Test
	public void testGetParametersWithBogusDate() throws Exception {

		String[] args = new String[] { "schedule.date(date)=20080123" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong date: " + message, contains(message, "20080123"));
			assertTrue("Message should contain format: " + message, contains(message, "yyyy/MM/dd"));
		}
	}

	@Test
	public void testGetParametersWithNumberFormat() throws Exception {

		String[] args = new String[] { "value(long)=1,000" };

		factory.setNumberFormat(new DecimalFormat("#,###"));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1000L, props.getLong("value"));
	}

	@Test
	public void testGetParametersWithBogusLong() throws Exception {

		String[] args = new String[] { "value(long)=foo" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong number: " + message, contains(message, "foo"));
			assertTrue("Message should contain format: " + message, contains(message, "#"));
		}
	}

	@Test
	public void testGetParametersWithDoubleValueDeclaredAsLong() throws Exception {

		String[] args = new String[] { "value(long)=1.03" };
		factory.setNumberFormat(new DecimalFormat("#.#"));

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong number: " + message, contains(message, "1.03"));
			assertTrue("Message should contain 'decimal': " + message, contains(message, "decimal"));
		}
	}

	@Test
	public void testGetParametersWithBogusDouble() throws Exception {

		String[] args = new String[] { "value(double)=foo" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong number: " + message, contains(message, "foo"));
			assertTrue("Message should contain format: " + message, contains(message, "#"));
		}
	}

	@Test
	public void testGetParametersWithDouble() throws Exception {

		String[] args = new String[] { "value(double)=1.38" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.38, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	public void testGetParametersWithDoubleAndLongAndNumberFormat() throws Exception {

		String[] args = new String[] { "value(double)=1,23456", "long(long)=123.456" };
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		factory.setNumberFormat(format);

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.23456, props.getDouble("value"), Double.MIN_VALUE);
		assertEquals(123456, props.getLong("long"));

	}

	@Test
	public void testGetParametersWithRoundDouble() throws Exception {

		String[] args = new String[] { "value(double)=1.0" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	public void testGetParametersWithVeryRoundDouble() throws Exception {

		String[] args = new String[] { "value(double)=1" };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1.0, props.getDouble("value"), Double.MIN_VALUE);
	}

	@Test
	public void testGetProperties() throws Exception {

		JobParameters parameters = new JobParametersBuilder().addDate("schedule.date", dateFormat.parse("01/23/2008"))
				.addString("job.key", "myKey").addLong("vendor.id", new Long(33243243)).addDouble("double.key", 1.23)
				.toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
		assertEquals("1.23", props.getProperty("double.key(double)"));
	}

	@Test
	public void testRoundTrip() throws Exception {

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
	public void testRoundTripWithNumberFormat() throws Exception {

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
	public void testEmptyArgs() {

		JobParameters props = factory.getJobParameters(new Properties());
		assertTrue(props.getParameters().isEmpty());
	}

	@Test
	public void testNullArgs() {
		assertEquals(new JobParameters(), factory.getJobParameters(null));
		assertEquals(new Properties(), factory.getProperties(null));
	}

	private boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}
	
	@Test
	public void testGetPropertiesWithNullValues() throws Exception {
		
		JobParameters parameters = new JobParametersBuilder().addDate("schedule.date", null)
				.addString("job.key", null).addLong("vendor.id", null).addDouble("double.key", null)
				.toJobParameters();
		
		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		
		final String NOT_FOUND = "NOT FOUND";
		assertEquals(NOT_FOUND, props.getProperty("schedule.date", NOT_FOUND));  
		assertEquals(NOT_FOUND, props.getProperty("job.key", NOT_FOUND));  
		assertEquals(NOT_FOUND, props.getProperty("vendor.id", NOT_FOUND));  
		assertEquals(NOT_FOUND, props.getProperty("double.key", NOT_FOUND));  
	}
}
