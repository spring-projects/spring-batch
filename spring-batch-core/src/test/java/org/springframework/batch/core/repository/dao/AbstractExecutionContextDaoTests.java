/*
 * Copyright 2008-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.repository.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ExecutionContextDao} implementations.
 */
public abstract class AbstractExecutionContextDaoTests extends AbstractTransactionalJUnit4SpringContextTests {

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	private ExecutionContextDao contextDao;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	@BeforeEach
	void setUp() {
		jobInstanceDao = getJobInstanceDao();
		jobExecutionDao = getJobExecutionDao();
		stepExecutionDao = getStepExecutionDao();
		contextDao = getExecutionContextDao();

		JobInstance ji = jobInstanceDao.createJobInstance("testJob", new JobParameters());
		jobExecution = new JobExecution(1L, ji, new JobParameters());
		jobExecutionDao.updateJobExecution(jobExecution);
		stepExecution = new StepExecution(1L, "stepName", jobExecution);
		stepExecutionDao.updateStepExecution(stepExecution);

	}

	/**
	 * @return Configured {@link ExecutionContextDao} implementation ready for use.
	 */
	protected abstract JobExecutionDao getJobExecutionDao();

	/**
	 * @return Configured {@link ExecutionContextDao} implementation ready for use.
	 */
	protected abstract JobInstanceDao getJobInstanceDao();

	/**
	 * @return Configured {@link ExecutionContextDao} implementation ready for use.
	 */
	protected abstract StepExecutionDao getStepExecutionDao();

	/**
	 * @return Configured {@link ExecutionContextDao} implementation ready for use.
	 */
	protected abstract ExecutionContextDao getExecutionContextDao();

	@Transactional
	@Test
	void testSaveAndFindJobContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object>singletonMap("key", "value"));
		jobExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(jobExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	void testSaveAndFindExecutionContexts() {

		List<StepExecution> stepExecutions = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			JobInstance ji = jobInstanceDao.createJobInstance("testJob" + i, new JobParameters());
			JobExecution je = new JobExecution(i, ji, new JobParameters());
			jobExecutionDao.updateJobExecution(je);
			StepExecution se = new StepExecution(1L, "step" + i, je);
			se.setStatus(BatchStatus.STARTED);
			se.setReadSkipCount(i);
			se.setProcessSkipCount(i);
			se.setWriteSkipCount(i);
			se.setProcessSkipCount(i);
			se.setRollbackCount(i);
			se.setLastUpdated(LocalDateTime.now());
			se.setReadCount(i);
			se.setFilterCount(i);
			se.setWriteCount(i);
			stepExecutions.add(se);
		}
		for (StepExecution stepExecution : stepExecutions) {
			stepExecutionDao.updateStepExecution(stepExecution);
		}
		contextDao.saveExecutionContexts(stepExecutions);

		for (int i = 0; i < 3; i++) {
			ExecutionContext retrieved = contextDao.getExecutionContext(stepExecutions.get(i).getJobExecution());
			assertEquals(stepExecutions.get(i).getExecutionContext(), retrieved);
		}
	}

	@Transactional
	@Test
	void testSaveNullExecutionContexts() {
		assertThrows(IllegalArgumentException.class, () -> contextDao.saveExecutionContexts(null));
	}

	@Transactional
	@Test
	void testSaveEmptyExecutionContexts() {
		contextDao.saveExecutionContexts(new ArrayList<>());
	}

	@Transactional
	@Test
	void testSaveAndFindEmptyJobContext() {

		ExecutionContext ctx = new ExecutionContext();
		jobExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(jobExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	void testUpdateContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object>singletonMap("key", "value"));
		jobExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(jobExecution);

		ctx.putLong("longKey", 7);
		contextDao.updateExecutionContext(jobExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}

	@Transactional
	@Test
	void testSaveAndFindStepContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object>singletonMap("key", "value"));
		stepExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(stepExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	void testSaveAndFindEmptyStepContext() {

		ExecutionContext ctx = new ExecutionContext();
		stepExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(stepExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	void testUpdateStepContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object>singletonMap("key", "value"));
		stepExecution.setExecutionContext(ctx);
		contextDao.saveExecutionContext(stepExecution);

		ctx.putLong("longKey", 7);
		contextDao.updateExecutionContext(stepExecution);

		ExecutionContext retrieved = contextDao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}

	@Transactional
	@Test
	void testStoreInteger() {

		ExecutionContext ec = new ExecutionContext();
		ec.put("intValue", 343232);
		stepExecution.setExecutionContext(ec);
		contextDao.saveExecutionContext(stepExecution);
		ExecutionContext restoredEc = contextDao.getExecutionContext(stepExecution);
		assertEquals(ec, restoredEc);
	}

	@Transactional
	@Test
	void testDeleteStepExecutionContext() {
		// given
		ExecutionContext ec = new ExecutionContext();
		stepExecution.setExecutionContext(ec);
		contextDao.saveExecutionContext(stepExecution);

		// when
		contextDao.deleteExecutionContext(stepExecution);

		// then
		ExecutionContext restoredEc = contextDao.getExecutionContext(stepExecution);
		// FIXME contextDao.getExecutionContext should return null and not an empty
		// context
		assertEquals(new ExecutionContext(), restoredEc);
	}

	@Transactional
	@Test
	void testDeleteJobExecutionContext() {
		// given
		ExecutionContext ec = new ExecutionContext();
		jobExecution.setExecutionContext(ec);
		contextDao.saveExecutionContext(jobExecution);

		// when
		contextDao.deleteExecutionContext(jobExecution);

		// then
		ExecutionContext restoredEc = contextDao.getExecutionContext(jobExecution);
		// FIXME contextDao.getExecutionContext should return null and not an empty
		// context
		assertEquals(new ExecutionContext(), restoredEc);
	}

}
