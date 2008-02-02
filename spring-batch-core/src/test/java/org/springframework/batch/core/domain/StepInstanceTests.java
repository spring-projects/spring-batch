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
package org.springframework.batch.core.domain;

import junit.framework.TestCase;

import org.springframework.batch.item.StreamContext;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 *
 */
public class StepInstanceTests extends TestCase {

	StepInstance instance = new StepInstance(new Long(13));

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#StepInstance()}.
	 */
	public void testStepInstance() {
		assertNull(new StepInstance().getId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getStepExecutionCount()}.
	 */
	public void testGetStepExecutionCount() {
		assertEquals(0, instance.getStepExecutionCount());
		instance.setStepExecutionCount(23);
		assertEquals(23, instance.getStepExecutionCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getStreamContext()}.
	 */
	public void testGetStreamContext() {
		assertNotNull(instance.getStreamContext());
		assertTrue(instance.getStreamContext().getProperties().isEmpty());
		instance.setStreamContext(new StreamContext(PropertiesConverter.stringToProperties("foo=bar")));
		assertEquals("bar", instance.getStreamContext().getProperties().getProperty("foo"));
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getStatus()}.
	 */
	public void testGetStatus() {
		assertEquals(null, instance.getStatus());
		instance.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, instance.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getJobInstance()}.
	 */
	public void testGetJobInstance() {
		assertEquals(null, instance.getJobInstance());
		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters());
		instance = new StepInstance(jobInstance, null);
		assertEquals(jobInstance, instance.getJobInstance());
	}
	
	public void testGetJob(){
		
		Job job = new JobSupport("job");
		JobInstance jobInstance = new JobInstance(new Long(2), new JobParameters(), job);
		instance = new StepInstance(jobInstance, null);
		assertEquals(job, instance.getJobInstance().getJob());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getName()}.
	 */
	public void testGetName() {
		assertEquals(null, instance.getName());
		instance = new StepInstance(null, "foo");
		assertEquals("foo", instance.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepInstance#getJobId()}.
	 */
	public void testGetJobId() {
		assertEquals(null, instance.getJobId());
		instance = new StepInstance(new JobInstance(new Long(23), new JobParameters()), null);
		assertEquals(23, instance.getJobId().longValue());
	}

	public void testEqualsWithSameIdentifier() throws Exception {
		JobInstance job = new JobInstance(new Long(100), new JobParameters());
		StepInstance step1 = new StepInstance(job, "foo", new Long(0));
		StepInstance step2 = new StepInstance(job, "foo", new Long(0));
		assertEquals(step1, step2);
	}

	public void testToString() throws Exception {
		assertTrue("Should contain name", instance.toString().indexOf("name=")>=0);
		assertTrue("Should contain status", instance.toString().indexOf("status=")>=0);
	}

}
