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

/**
 * @author dsyer
 *
 */
public class JobInstanceTests extends TestCase {

	private JobInstance instance = new JobInstance(new Long(11), new JobParameters(), new JobSupport("job"));
	
	public void testLastExecution(){
		JobExecution lastExecution = new JobExecution();
		assertNull(instance.getLastExecution());
		instance.setLastExecution(lastExecution);
		assertEquals(lastExecution, instance.getLastExecution());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getJobExecutionCount()}.
	 */
	public void testGetJobExecutionCount() {
		assertEquals(0, instance.getJobExecutionCount());
		instance.setJobExecutionCount(22);
		assertEquals(22, instance.getJobExecutionCount());
	}
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getIdentifier()}.
	 */
	public void testGetName() {
		instance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("foo"));
		assertEquals("foo", instance.getJobName());
	}
	
	public void testGetJob(){
		assertEquals("job", instance.getJob().getName());
		instance.setJob(null);
		assertEquals(null, instance.getJob());
	}

	public void testCreateJobExecution(){
		JobExecution execution = instance.createJobExecution();
		assertNotNull(execution);
		assertEquals(execution, instance.getLastExecution());
	}

	public void testCreateWithNulls(){
		instance = new JobInstance(null, null);
		assertEquals(null, instance.getJobName());
		assertEquals(0, instance.getJobParameters().getParameters().size());
	}
}
