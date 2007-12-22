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

import java.util.Date;

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
	public void testDefaultConstructor() {
		instance = new ScheduledJobIdentifier();
		assertEquals(null, instance.getName());
		instance.setName("foo");
		assertEquals("foo", instance.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getName()}.
	 */
	public void testGetName() {
		assertEquals(null, instance.getName());
		instance.setName("foo");
		assertEquals("foo", instance.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getJobKey()}.
	 */
	public void testGetJobStream() {
		assertEquals("", instance.getJobKey());
		instance.setJobKey("foo");
		assertEquals("foo", instance.getJobKey());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getScheduleDate()}.
	 */
	public void testGetScheduleDate() {
		assertNotNull(instance.getScheduleDate());
		instance.setScheduleDate(new Date(100L));
		assertEquals(100L, instance.getScheduleDate().getTime());
	}
	
	public void testEqualsSelf() throws Exception {
		assertEquals(instance, instance);
	}

	public void testEqualsInstanceWithSameProperties() throws Exception {
		ScheduledJobIdentifier other = new ScheduledJobIdentifier(instance.getName());
		other.setJobKey(instance.getJobKey());
		other.setScheduleDate(instance.getScheduleDate());
		assertEquals(instance, other);
		assertEquals(instance.hashCode(), other.hashCode());
	}

	public void testEqualsInstanceWithTimestamp() throws Exception {
		ScheduledJobIdentifier other = new ScheduledJobIdentifier(instance.getName());
		other.setJobKey(instance.getJobKey());
		other.setScheduleDate(new Date(instance.getScheduleDate().getTime()));
		assertEquals(instance, other);
		assertEquals(other, instance);
		assertEquals(instance.hashCode(), other.hashCode());
	}

	public void testEqualsNull() throws Exception {
		assertNotSame(null, instance);
	}
	
	public void testToString() throws Exception {
		assertTrue("String does not contain key: "+instance, instance.toString().indexOf("key=")>=0);
		assertTrue("String does not contain date: "+instance, instance.toString().indexOf("scheduleDate=")>=0);
	}
}
