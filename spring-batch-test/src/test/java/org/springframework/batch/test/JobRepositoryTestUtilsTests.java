/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
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
	void testMandatoryProperties() {
		utils = new JobRepositoryTestUtils();
		assertThrows(IllegalArgumentException.class, utils::afterPropertiesSet);
	}

	@Test
	void testMandatoryDataSource() {
		utils = new JobRepositoryTestUtils();
		utils.setJobRepository(jobRepository);
		assertThrows(IllegalArgumentException.class, utils::afterPropertiesSet);
	}

	@Test
	void testCreateJobExecutions() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
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
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		List<JobExecution> list = new ArrayList<>();
		JobExecution jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		jobExecution.setEndTime(new Date());
		list.add(jobExecution);
		jobRepository.update(jobExecution);
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		list.add(jobExecution);
		assertEquals(beforeJobs + 2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testCreateJobExecutionsByName() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		List<JobExecution> list = utils.createJobExecutions("foo", new String[] { "bar", "spam" }, 3);
		assertEquals(3, list.size());
		assertEquals(beforeJobs + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps + 6, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
	}

	@Test
	void testRemoveJobExecutionsIncrementally() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		List<JobExecution> list1 = utils.createJobExecutions(3);
		List<JobExecution> list2 = utils.createJobExecutions(2);
		assertEquals(beforeJobs + 5, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		utils.removeJobExecutions(list2);
		assertEquals(beforeJobs + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		utils.removeJobExecutions(list1);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testCreateJobExecutionsWithIncrementer() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		utils.setJobParametersIncrementer(new JobParametersIncrementer() {
			@Override
			public JobParameters getNext(@Nullable JobParameters parameters) {
				return new JobParametersBuilder().addString("foo", "bar").toJobParameters();
			}
		});
		List<JobExecution> list = utils.createJobExecutions(1);
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0).getJobParameters().getString("foo"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

}
