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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.util.StringUtils;

/**
 * @author Lucas Ward
 *
 */
public class ScheduledJobParametersFactoryTests extends TestCase {

	ScheduledJobParametersFactory factory;
	
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
	
	protected void setUp() throws Exception {
		super.setUp();
		
		factory = new ScheduledJobParametersFactory();
	}
	
	public void testGetProperties() throws Exception{
		
		String jobKey = "job.key=myKey";
		String scheduleDate = "schedule.date=2008/01/23";
		String vendorId = "vendor.id=33243243";
		
		String[] args = new String[]{jobKey, scheduleDate, vendorId};

		JobParameters props = factory.getJobParameters(StringUtils.splitArrayElementsIntoProperties(args, "="));
		assertNotNull(props);
		assertEquals("myKey", props.getString("job.key"));
		assertEquals("33243243", props.getString("vendor.id"));
		Date date = dateFormat.parse("01/23/2008");
		assertEquals(date, props.getDate("schedule.date"));
	}
		
	public void testEmptyArgs(){
		
		JobParameters props = factory.getJobParameters(new Properties());
		assertTrue(props.getParameters().isEmpty());
	}
}
