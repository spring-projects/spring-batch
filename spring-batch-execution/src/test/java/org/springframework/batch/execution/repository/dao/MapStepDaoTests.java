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

import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;

public class MapStepDaoTests extends TestCase {

	MapStepDao dao = new MapStepDao();
	private JobInstance job;
	private StepInstance step;
	
	// Make sure we get a new job for each test...
	static long jobId=100;
	
	protected void setUp() throws Exception {
		MapStepDao.clear();
		job = new JobInstance(new Long(jobId++), new JobParameters());
		step = dao.createStep(job, "foo");	
	}
	
	public void testCreateUnequal() throws Exception {
		StepInstance step2 = dao.createStep(job, "foo");;
		assertFalse(step.equals(step2));
		assertFalse(step.hashCode()==step2.hashCode());
	}

	public void testCreateAndRetrieveSingle() throws Exception {
		StepInstance result = dao.findStep(job, "foo");
		assertEquals(step, result);
	}
	
	public void testCreateAndRetrieveSingleWhenMultipleStored() throws Exception {
		dao.createStep(job, "bar");;
		StepInstance result = dao.findStep(job, "foo");
		assertEquals(step, result);
	}
	
	public void testCreateAndRetrieveSingleFromList() throws Exception {
		List result = dao.findSteps(job);
		assertTrue(result.contains(step));
	}

	public void testCreateAndRetrieveMultiple() throws Exception {
		step = dao.createStep(job, "bar");
		List result = dao.findSteps(job);
		assertEquals(2, result.size());
		assertTrue(result.contains(step));		
	}
	
	public void testFindWithEmptyResults() throws Exception {
		List result = dao.findSteps(new JobInstance(new Long(22), new JobParameters()));
		assertEquals(0, result.size());		
	}
	
	public void testFindSingleWithEmptyResults() throws Exception {
		StepInstance result = dao.findStep(new JobInstance(new Long(22), new JobParameters()), "bar");
		assertEquals(null, result);		
	}

	public void testNoExecutionsForNew() throws Exception {
		assertEquals(0, dao.getStepExecutionCount(step));
	}

	public void testSaveExecutionUpdatesId() throws Exception {
		StepExecution execution = new StepExecution(step, null, null);
		assertNull(execution.getId());
		dao.save(execution);
		assertNotNull(execution.getId());
	}

	public void testCorrectExecutionCountForExisting() throws Exception {
		dao.save(new StepExecution(step, null, null));
		assertEquals(1, dao.getStepExecutionCount(step));
	}
	
	public void testOnlyOneExecutionPerStep() throws Exception {
		dao.save(new StepExecution(step, null, null));
		dao.save(new StepExecution(step, null, null));
		assertEquals(2, dao.getStepExecutionCount(step));
	}

	public void testSaveRestartData() throws Exception {
		assertEquals(null, dao.getStreamContext(step.getId()));
		step.setStatus(BatchStatus.COMPLETED);
		Properties data = new Properties();
		data.setProperty("restart.key1", "restartData");
		StreamContext streamContext = new GenericStreamContext(data);
		step.setStreamContext(streamContext);
		dao.update(step);
		StepInstance tempStep = dao.findStep(job, step.getName());
		assertEquals(tempStep, step);
		assertEquals(tempStep.getStreamContext().getProperties().toString(), 
				streamContext.getProperties().toString());
	}

}
