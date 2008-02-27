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

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.batch.execution.step.StepSupport;

public class MapStepDaoTests extends TestCase {

	MapStepDao dao = new MapStepDao();

	private JobInstance job;

	private Step step;

	// Make sure we get a new job for each test...
	static long jobId = 100;

	protected void setUp() throws Exception {
		MapStepDao.clear();
		job = new JobInstance(new Long(jobId++), new JobParameters(), new JobSupport("testJob"));
		step = new StepSupport("foo");
	}

	public void testSaveExecutionUpdatesId() throws Exception {
		StepExecution execution = new StepExecution(step, new JobExecution(new JobInstance(new Long(1),
				new JobParameters(), new JobSupport("jobName"))));
		assertNull(execution.getId());
		dao.saveStepExecution(execution);
		assertNotNull(execution.getId());
	}

	public void testSaveExecutionContext() throws Exception {
		// JobExecution jobExecution = new JobExecution(null);
		// StepExecution stepExecution = new StepExecution(step, jobExecution,
		// null);
		// assertEquals(null, dao.findExecutionContext(stepExecution));
		// Properties data = new Properties();
		// data.setProperty("restart.key1", "restartData");
		// ExecutionContext executionContext = new ExecutionContext(data);
		// stepExecution.setExecutionContext(executionContext);
		// dao.saveStepExecution(stepExecution);
		// StepExecution tempExecution = dao.getStepExecution(jobExecution,
		// step);
		// assertEquals(tempExecution, stepExecution);
		// assertEquals(stepExecution.getExecutionContext(),
		// tempExecution.getExecutionContext());
	}

}
