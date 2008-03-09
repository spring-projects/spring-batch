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

package org.springframework.batch.core.repository.support.dao;

import java.util.HashMap;

import junit.framework.TestCase;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.support.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.dao.StepExecutionDao;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Tests for {@link MapStepExecutionDao}.
 */
public class MapStepExecutionDaoTests extends TestCase {

	private StepExecutionDao dao = new MapStepExecutionDao();

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private Step step;

	private StepExecution stepExecution;

	protected void setUp() throws Exception {
		MapStepExecutionDao.clear();
		jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("testJob"));
		jobExecution = new JobExecution(jobInstance, new Long(1));
		step = new StepSupport("foo");
		stepExecution = new StepExecution(step, jobExecution);
	}

	public void testSaveExecutionAssignsIdAndVersion() throws Exception {
		assertNull(stepExecution.getId());
		assertNull(stepExecution.getVersion());
		dao.saveStepExecution(stepExecution);
		assertNotNull(stepExecution.getId());
		assertNotNull(stepExecution.getVersion());
	}

	public void testSaveAndFindExecution() {
		stepExecution.setStatus(BatchStatus.STARTED);
		dao.saveStepExecution(stepExecution);

		StepExecution retrieved = dao.getStepExecution(jobExecution, step);
		assertEquals(stepExecution, retrieved);
		assertEquals(BatchStatus.STARTED, retrieved.getStatus());

		assertNull(dao.getStepExecution(jobExecution, new StepSupport("not-existing step")));
	}

	public void testGetForNotExistingJobExecution() {
		assertNull(dao.getStepExecution(new JobExecution(jobInstance, new Long(777)), step));
	}

	/**
	 * To-be-saved execution must not already have an id.
	 */
	public void testSaveExecutionWithIdAlreadySet() {
		stepExecution.setId(new Long(7));
		try {
			dao.saveStepExecution(stepExecution);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * To-be-saved execution must not already have a version.
	 */
	public void testSaveExecutionWithVersionAlreadySet() {
		stepExecution.incrementVersion();
		try {
			dao.saveStepExecution(stepExecution);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Update and retrieve updated StepExecution - make sure the update is
	 * reflected as expected and version number has been incremented
	 */
	public void testUpdateExecution() {
		stepExecution.setStatus(BatchStatus.STARTED);
		dao.saveStepExecution(stepExecution);
		Integer versionAfterSave = stepExecution.getVersion();

		stepExecution.setStatus(BatchStatus.STOPPED);
		dao.updateStepExecution(stepExecution);
		assertEquals(versionAfterSave.intValue() + 1, stepExecution.getVersion().intValue());

		StepExecution retrieved = dao.getStepExecution(jobExecution, step);
		assertEquals(stepExecution, retrieved);
		assertEquals(BatchStatus.STOPPED, retrieved.getStatus());
	}

	public void testSaveAndFindContext() {
		ExecutionContext ctx = new ExecutionContext(new HashMap() {
			{
				put("key", "value");
			}
		});
		stepExecution.setExecutionContext(ctx);
		dao.saveOrUpdateExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.findExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	public void testUpdateContext() {
		ExecutionContext ctx = new ExecutionContext(new HashMap() {
			{
				put("key", "value");
			}
		});
		stepExecution.setExecutionContext(ctx);
		dao.saveOrUpdateExecutionContext(stepExecution);

		ctx.putLong("longKey", 7);
		dao.saveOrUpdateExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.findExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}

	/**
	 * Exception should be raised when the version of update argument doesn't
	 * match the version of persisted entity.
	 */
	public void testConcurrentModificationException() {
		jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("testJob"));
		jobExecution = new JobExecution(jobInstance, new Long(1));
		step = new StepSupport("foo");

		StepExecution exec1 = new StepExecution(step, jobExecution);
		dao.saveStepExecution(exec1);

		StepExecution exec2 = new StepExecution(step, jobExecution);
		exec2.setId(exec1.getId());

		exec2.incrementVersion();
		assertEquals(new Integer(0), exec1.getVersion());
		assertEquals(exec1.getVersion(), exec2.getVersion());

		dao.updateStepExecution(exec1);
		assertEquals(new Integer(1), exec1.getVersion());

		try {
			dao.updateStepExecution(exec2);
			fail();
		}
		catch (OptimisticLockingFailureException e) {
			// expected
		}

	}

}
