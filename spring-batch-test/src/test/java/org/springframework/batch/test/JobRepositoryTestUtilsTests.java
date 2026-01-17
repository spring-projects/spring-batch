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
package org.springframework.batch.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
@SpringJUnitConfig(locations = "/simple-job-launcher-context.xml")
class JobRepositoryTestUtilsTests {

	private JobRepositoryTestUtils utils;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private int beforeJobs;

	private int beforeSteps;

	@BeforeEach
	void init() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		beforeJobs = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION");
		beforeSteps = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION");
	}

	@Test
	void testCreateJobExecutions() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository);
		List<JobExecution> list = utils.createJobExecutions(3);
		assertEquals(3, list.size());
		assertEquals(beforeJobs + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
	}

	@Test
	void testRemoveJobExecutionsWithSameJobInstance() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository);
		List<JobExecution> list = new ArrayList<>();
		JobParameters jobParameters = new JobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);

		jobExecution.setEndTime(LocalDateTime.now());
		jobExecution.setStatus(BatchStatus.COMPLETED);
		list.add(jobExecution);
		jobRepository.update(jobExecution);
		jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		list.add(jobExecution);
		assertEquals(beforeJobs + 2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testCreateJobExecutionsByName() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository);
		List<JobExecution> list = utils.createJobExecutions("foo", new String[] { "bar", "spam" }, 3);
		assertEquals(3, list.size());
		assertEquals(beforeJobs + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps + 6, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
	}

	@Test
	void testCreateJobExecutionsWithIncrementer() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository);
		utils.setJobParametersIncrementer(
				parameters -> new JobParametersBuilder().addString("foo", "bar").toJobParameters());
		List<JobExecution> list = utils.createJobExecutions(1);
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0).getJobParameters().getString("foo"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testRemoveJobExecutions() throws Exception {
		// given
		utils = new JobRepositoryTestUtils(jobRepository);
		utils.createJobExecutions("foo", new String[] { "step1", "step2" }, 1);
		assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));

		// when
		utils.removeJobExecutions();

		// then
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testRemoveJobExecution() throws Exception {
		// given
		utils = new JobRepositoryTestUtils(jobRepository);
		JobExecution jobExecution = utils.createJobExecutions(1).get(0);
		assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));

		// when
		jobExecution.setVersion(-1); // simulate stale version
		utils.removeJobExecution(jobExecution);

		// then
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));

		List<JobExecution> jobExecutions = utils.createJobExecutions("test", new String[] { "step1", "step2" }, 2);
		assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(4, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));

		// when
		jobExecutions.forEach(je -> je.setVersion(-1)); // simulate stale version
		utils.removeJobExecutions(jobExecutions);

		// then
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

}
