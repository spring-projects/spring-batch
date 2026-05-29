/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.core.repository.dao.jdbc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcJobInstanceDaoTests {

	private JdbcJobExecutionDao jdbcJobExecutionDao;

	private JdbcJobInstanceDao jdbcJobInstanceDao;

	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() throws Exception {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
			.addScript("/org/springframework/batch/core/schema-h2.sql")
			.build();
		jdbcTemplate = new JdbcTemplate(database);
		jdbcJobInstanceDao = new JdbcJobInstanceDao();
		jdbcJobInstanceDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobInstanceIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_INSTANCE_SEQ");
		jdbcJobInstanceDao.setJobInstanceIncrementer(jobInstanceIncrementer);
		jdbcJobInstanceDao.afterPropertiesSet();

		jdbcJobExecutionDao = new JdbcJobExecutionDao();
		jdbcJobExecutionDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_EXECUTION_SEQ");
		jdbcJobExecutionDao.setJobExecutionIncrementer(jobExecutionIncrementer);
		jdbcJobExecutionDao.setJobInstanceDao(jdbcJobInstanceDao);
		jdbcJobExecutionDao.afterPropertiesSet();
	}

	@Test
	void testCreateJobInstance() {
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", new JobParameters());

		Assertions.assertNotNull(jobInstance);
		assertEquals("job", jobInstance.getJobName());
		assertEquals(1, jobInstance.getId());
		assertEquals(0, jobInstance.getJobExecutions().size());
	}

	@Test
	void testGetJobInstance() {
		jdbcJobInstanceDao.createJobInstance("job", new JobParameters());

		JobInstance jobInstance = jdbcJobInstanceDao.getJobInstance(1L);

		Assertions.assertNotNull(jobInstance);
		assertEquals("job", jobInstance.getJobName());
		assertEquals(1, jobInstance.getId());
		assertEquals(0, jobInstance.getJobExecutions().size());
	}

	@Test
	void testGetJobNames() {
		jdbcJobInstanceDao.createJobInstance("job", new JobParameters());

		List<String> jobNames = jdbcJobInstanceDao.getJobNames();

		assertEquals(1, jobNames.size());
		assertEquals("job", jobNames.get(0));
	}

	@Test
	void testFindJobInstanceByExecution() {
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("testInstance", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		JobInstance returnedInstance = jdbcJobInstanceDao.getJobInstance(jobExecution);
		assertEquals(jobInstance, returnedInstance);
	}

	@Test
	void testHexing() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] bytes = digest.digest("f78spx".getBytes(StandardCharsets.UTF_8));
		StringBuilder output = new StringBuilder();
		for (byte bite : bytes) {
			output.append(String.format("%02x", bite));
		}
		assertEquals(32, output.length(), "Wrong hash: " + output);
		String value = String.format("%032x", new BigInteger(1, bytes));
		assertEquals(32, value.length(), "Wrong hash: " + value);
		assertEquals(value, output.toString());
	}

	@Test
	void testJobInstanceWildcard() {
		jdbcJobInstanceDao.createJobInstance("anotherJob", new JobParameters());
		jdbcJobInstanceDao.createJobInstance("someJob", new JobParameters());

		List<JobInstance> jobInstances = jdbcJobInstanceDao.getJobInstances("*Job", 0, 2);
		assertEquals(2, jobInstances.size());

		for (JobInstance instance : jobInstances) {
			assertTrue(instance.getJobName().contains("Job"));
		}

		jobInstances = jdbcJobInstanceDao.getJobInstances("Job*", 0, 2);
		assertTrue(jobInstances.isEmpty());
	}

	@Test
	void testDeleteJobInstance() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("someTestInstance", jobParameters);

		// when
		jdbcJobInstanceDao.deleteJobInstance(jobInstance);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE"));
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Test
	void testGetMissingById() {
		JobInstance retrievedInstance = jdbcJobInstanceDao.getJobInstance(1111111L);
		assertNull(retrievedInstance);

	}

	@Test
	void testGetLastInstance() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString("name", "bar").toJobParameters();
		JobParameters jobParameters3 = new JobParameters();
		jdbcJobInstanceDao.createJobInstance("job", jobParameters1);
		JobInstance jobInstance2 = jdbcJobInstanceDao.createJobInstance("job", jobParameters2);
		jdbcJobInstanceDao.createJobInstance("anotherJob", jobParameters3);
		JobInstance lastJobInstance = jdbcJobInstanceDao.getLastJobInstance("job");
		assertEquals(jobInstance2, lastJobInstance);
	}

	@Test
	void testGetLastInstanceWhenNoInstance() {
		JobInstance lastJobInstance = jdbcJobInstanceDao.getLastJobInstance("NonExistingJob");
		assertNull(lastJobInstance);
	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
	@Test
	void testCreateDuplicateInstance() {
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		jdbcJobInstanceDao.createJobInstance("job", jobParameters);

		assertThrows(IllegalStateException.class, () -> jdbcJobInstanceDao.createJobInstance("job", jobParameters));
	}

}
