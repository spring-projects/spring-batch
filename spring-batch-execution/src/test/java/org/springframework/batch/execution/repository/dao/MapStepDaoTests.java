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

package org.springframework.batch.execution.repository.dao;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;

public class MapStepDaoTests extends TestCase {

	MapStepDao dao = new MapStepDao();
	private JobInstance job;
	private String step;
	
	// Make sure we get a new job for each test...
	static long jobId=100;
	
	protected void setUp() throws Exception {
		MapStepDao.clear();
		job = new JobInstance(new Long(jobId++), new JobParameters());
		step = "foo";	
	}

	public void testNoExecutionsForNew() throws Exception {
//		assertEquals(0, dao.getStepExecutionCount(step));
	}

	public void testSaveExecutionUpdatesId() throws Exception {
		StepExecution execution = new StepExecution(step, null, null);
		assertNull(execution.getId());
		dao.saveStepExecution(execution);
		assertNotNull(execution.getId());
	}

	public void testCorrectExecutionCountForExisting() throws Exception {
//		dao.saveStepExecution(new StepExecution(step, null, null));
//		assertEquals(1, dao.getStepExecutionCount(step));
	}
	
	public void testOnlyOneExecutionPerStep() throws Exception {
//		dao.saveStepExecution(new StepExecution(step, null, null));
//		dao.saveStepExecution(new StepExecution(step, null, null));
//		assertEquals(2, dao.getStepExecutionCount(step));
	}

	public void testSaveExecutionContext() throws Exception {
//		assertEquals(null, dao.getExecutionContext(step.getId()));
//		Properties data = new Properties();
//		data.setProperty("restart.key1", "restartData");
//		ExecutionContext executionContext = new ExecutionContext(data);
//		StepExecution stepExecution = new StepExecution(step, null, null);
//		stepExecution.setExecutionContext(executionContext);
//		dao.saveStepExecution(stepExecution);
//		StepExecution tempExecution = dao.getStepExecution(stepExecution.getId(), step);
//		assertEquals(tempExecution, stepExecution);
//		assertEquals(stepExecution.getExecutionContext(), tempExecution.getExecutionContext());
	}

}
