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

import java.sql.Timestamp;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class JobExecutionTests extends TestCase {

	private JobExecution execution = new JobExecution(new Long(11));
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#JobExecution()}.
	 */
	public void testJobExecution() {
		assertNull(new JobExecution().getId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getEndTime()}.
	 */
	public void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Timestamp(100L));
		assertEquals(100L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getStartTime()}.
	 */
	public void testGetStartTime() {
		assertNotNull(execution.getStartTime());
		execution.setStartTime(new Timestamp(0L));
		assertEquals(0L, execution.getStartTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getStatus()}.
	 */
	public void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getJobId()}.
	 */
	public void testGetJobId() {
		assertEquals(11, execution.getJobId().longValue());
		execution = new JobExecution(new Long(23));
		assertEquals(23, execution.getJobId().longValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getExitCode()}.
	 */
	public void testGetExitCode() {
		assertEquals("", execution.getExitCode());
		execution.setExitCode("23");
		assertEquals("23", execution.getExitCode());
	}

}
