/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.test.repository;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml" })
// TODO refactor using black-box testing instead of white-box testing
@Disabled
class JdbcJobRepositoryTests {

	private JobSupport job;

	private final Set<Long> jobExecutionIds = new HashSet<>();

	private final Set<Long> jobIds = new HashSet<>();

	private final List<Serializable> list = new ArrayList<>();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobRepository repository;

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@BeforeEach
	void onSetUpInTransaction() {
		job = new JobSupport("test-job");
		job.setRestartable(true);
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT",
				"BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS", "BATCH_JOB_INSTANCE");
	}

	@Test
	void testFindOrCreateJob() throws Exception {
		job.setName("foo");
		int before = 0;
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance(job.getName(), jobParameters);
		JobExecution execution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");
		assertEquals(before + 1, after);
		assertNotNull(execution.getId());
	}

	@Test
	void testFindOrCreateJobWithExecutionContext() throws Exception {
		job.setName("foo");
		int before = 0;
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance(job.getName(), jobParameters);
		JobExecution execution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		execution.getExecutionContext().put("foo", "bar");
		repository.updateExecutionContext(execution);
		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT");
		assertEquals(before + 1, after);
		assertNotNull(execution.getId());
		JobExecution last = repository.getLastJobExecution(job.getName(), new JobParameters());
		assertEquals(execution, last);
		assertEquals(execution.getExecutionContext(), last.getExecutionContext());
	}

	@Test
	void testFindOrCreateJobConcurrently() {

		job.setName("bar");

		int before = 0;
		assertEquals(0, before);

		long t0 = System.currentTimeMillis();
		assertThrows(JobExecutionAlreadyRunningException.class, this::doConcurrentStart);
		long t1 = System.currentTimeMillis();

		JobExecution execution = (JobExecution) list.get(0);

		assertNotNull(execution);

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");
		assertNotNull(execution.getId());
		assertEquals(before + 1, after);

		logger.info("Duration: " + (t1 - t0)
				+ " - the second transaction did not block if this number is less than about 1000.");
	}

	@Test
	void testFindOrCreateJobConcurrentlyWhenJobAlreadyExists() throws Exception {

		job = new JobSupport("test-job");
		job.setRestartable(true);
		job.setName("spam");

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance(job.getName(), jobParameters);
		JobExecution execution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		cacheJobIds(execution);
		execution.setEndTime(LocalDateTime.now());
		repository.update(execution);
		execution.setStatus(BatchStatus.FAILED);

		int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");
		assertEquals(1, before);

		long t0 = System.currentTimeMillis();
		assertThrows(JobExecutionAlreadyRunningException.class, this::doConcurrentStart);
		long t1 = System.currentTimeMillis();

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");
		assertNotNull(execution.getId());
		assertEquals(before, after);

		logger.info("Duration: " + (t1 - t0)
				+ " - the second transaction did not block if this number is less than about 1000.");
	}

	private void cacheJobIds(JobExecution execution) {
		if (execution == null) {
			return;
		}
		jobExecutionIds.add(execution.getId());
		jobIds.add(execution.getJobInstanceId());
	}

	private JobExecution doConcurrentStart() throws Exception {
		new Thread(() -> {

			try {
				JobParameters jobParameters = new JobParameters();
				JobInstance jobInstance = repository.createJobInstance(job.getName(), jobParameters);
				JobExecution execution = repository.createJobExecution(jobInstance, jobParameters,
						new ExecutionContext());

				// simulate running execution
				execution.setStartTime(LocalDateTime.now());
				repository.update(execution);

				cacheJobIds(execution);
				list.add(execution);
				Thread.sleep(1000);
			}
			catch (Exception e) {
				list.add(e);
			}

		}).start();

		Thread.sleep(400);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance(job.getName(), jobParameters);
		JobExecution execution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		cacheJobIds(execution);

		int count = 0;
		while (list.size() == 0 && count++ < 100) {
			Thread.sleep(200);
		}

		assertEquals(1, list.size(), "Timed out waiting for JobExecution to be created");
		assertTrue(list.get(0) instanceof JobExecution, "JobExecution not created in thread: " + list.get(0));
		return (JobExecution) list.get(0);
	}

}
