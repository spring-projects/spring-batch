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
package org.springframework.batch.execution.bootstrap.support;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 
 */
public class DefaultJobParametersFactoryTests extends TestCase {

	DefaultJobParametersFactory factory = new DefaultJobParametersFactory();

	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	public void testGetParameters() throws Exception {

		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date(date)=2008/01/23";
		String vendorId = "vendor.id(long)=33243243";

		String[] args = new String[] { jobKey, scheduleDate, vendorId };

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals(new Long(33243243L), props.getLong("vendor.id"));
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	public void testGetParametersWithDateFormat() throws Exception {

		String[] args = new String[] { "schedule.date(date)=2008/23/01" };

		factory.setDateFormat(new SimpleDateFormat("yyyy/dd/MM"));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}

	public void testGetParametersWithBogusDate() throws Exception {

		String[] args = new String[] { "schedule.date(date)=20080123" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong date: "+message, message.contains("20080123"));
			assertTrue("Message should contain format: "+message, message.contains("yyyy/MM/dd"));
		}
	}

	public void testGetParametersWithNumberFormat() throws Exception {

		String[] args = new String[] { "value(long)=1,000" };

		factory.setNumberFormat(new DecimalFormat("#,###"));
		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals(1000L, props.getLong("value").longValue());
	}

	public void testGetParametersWithBogusLong() throws Exception {

		String[] args = new String[] { "value(long)=foo" };

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong number: "+message, message.contains("foo"));
			assertTrue("Message should contain format: "+message, message.contains("#"));
		}
	}

	public void testGetParametersWithDouble() throws Exception {

		String[] args = new String[] { "value(long)=1.03" };
		factory.setNumberFormat(new DecimalFormat("#.#"));

		try {
			factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain wrong number: "+message, message.contains("1.03"));
			assertTrue("Message should contain 'decimal': "+message, message.contains("decimal"));
		}
	}

	public void testGetProperties() throws Exception {

		JobParameters parameters = new JobParametersBuilder().addDate("schedule.date", dateFormat.parse("01/23/2008"))
				.addString("job.key", "myKey").addLong("vendor.id", new Long(33243243)).toJobParameters();

		Properties props = factory.getProperties(parameters);
		assertNotNull(props);
		assertEquals("myKey", props.getProperty("job.key"));
		assertEquals("33243243", props.getProperty("vendor.id(long)"));
		assertEquals("2008/01/23", props.getProperty("schedule.date(date)"));
	}

	public void testEmptyArgs() {

		JobParameters props = factory.getJobParameters(new Properties());
		assertTrue(props.getParameters().isEmpty());
	}
	
	public void testNullArgs(){
		assertEquals(new JobParameters(), factory.getJobParameters(null));
		assertEquals(new Properties(), factory.getProperties(null));
	}
}
