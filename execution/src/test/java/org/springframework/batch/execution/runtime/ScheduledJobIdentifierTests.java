/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.runtime;

import java.sql.Date;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class ScheduledJobIdentifierTests extends TestCase {

	private ScheduledJobIdentifier instance = new ScheduledJobIdentifier(null);

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getName()}.
	 */
	public void testGetName() {
		assertEquals(null, instance.getName());
		instance.setName("foo");
		assertEquals("foo", instance.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getJobStream()}.
	 */
	public void testGetJobStream() {
		assertEquals("", instance.getJobStream());
		instance.setJobStream("foo");
		assertEquals("foo", instance.getJobStream());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getScheduleDate()}.
	 */
	public void testGetScheduleDate() {
		assertNotNull(instance.getScheduleDate());
		instance.setScheduleDate(new Date(100L));
		assertEquals(100L, instance.getScheduleDate().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getJobRun()}.
	 */
	public void testGetJobRun() {
		assertEquals(0, instance.getJobRun());
		instance.setJobRun(1);
		assertEquals(1, instance.getJobRun());
	}

	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getLabel()}.
	 */
	public void testDefaultGetLabel() throws Exception {
		assertEquals("null--0-19700101", instance.getLabel());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getLabel()}.
	 */
	public void testGetLabelWithAllProperties() throws Exception {
		instance.setName("foo");
		instance.setJobStream("bar");
		instance.setJobRun(11);
		instance.setScheduleDate(new SimpleDateFormat("yyyyMMdd").parse("20070730"));
		assertEquals("foo-bar-11-20070730", instance.getLabel());
	}
}
