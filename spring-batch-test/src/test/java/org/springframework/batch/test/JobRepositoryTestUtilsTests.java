/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/simple-job-launcher-context.xml")
public class JobRepositoryTestUtilsTests {

	private JobRepositoryTestUtils utils;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private int beforeJobs;

	private int beforeSteps;

	@Before
	public void init() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		beforeJobs = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION");
		beforeSteps = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryProperties() throws Exception {
		utils = new JobRepositoryTestUtils();
		utils.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryDataSource() throws Exception {
		utils = new JobRepositoryTestUtils();
		utils.setJobRepository(jobRepository);
		utils.afterPropertiesSet();
	}

	@Test
	public void testCreateJobExecutions() throws Exception {
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
	public void testRemoveJobExecutionsWithSameJobInstance() throws Exception {
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
	public void testCreateJobExecutionsByName() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		List<JobExecution> list = utils.createJobExecutions("foo",new String[] {"bar", "spam"}, 3);
		assertEquals(3, list.size());
		assertEquals(beforeJobs + 3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps + 6, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		assertEquals(beforeSteps, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
	}

	@Test
	public void testRemoveJobExecutionsIncrementally() throws Exception {
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
	public void testCreateJobExecutionsWithIncrementer() throws Exception {
		utils = new JobRepositoryTestUtils(jobRepository, dataSource);
		utils.setJobParametersIncrementer(new JobParametersIncrementer() {
			@Override
			public JobParameters getNext(@Nullable JobParameters parameters) {
				return new JobParametersBuilder().addString("foo","bar").toJobParameters();
			}
		});
		List<JobExecution> list = utils.createJobExecutions(1);
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0).getJobParameters().getString("foo"));
		utils.removeJobExecutions(list);
		assertEquals(beforeJobs, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}
}
