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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.dao.AbstractJobInstanceDaoTests;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringJUnitConfig(locations = "sql-dao-test.xml")
public class JdbcJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	private JobInstanceDao jobInstanceDao;

	@Autowired
	private JobExecutionDao jobExecutionDao;

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT",
				"BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS", "BATCH_JOB_EXECUTION", "BATCH_JOB_INSTANCE");
		return jobInstanceDao;
	}

	@Transactional
	@Test
	void testFindJobInstanceByExecution() {

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = dao.createJobInstance("testInstance", jobParameters);
		JobExecution jobExecution = new JobExecution(jobInstance, 2L, jobParameters);
		jobExecutionDao.saveJobExecution(jobExecution);

		JobInstance returnedInstance = dao.getJobInstance(jobExecution);
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
		dao.createJobInstance("anotherJob", new JobParameters());
		dao.createJobInstance("someJob", new JobParameters());

		List<JobInstance> jobInstances = dao.findJobInstancesByName("*Job", 0, 2);
		assertEquals(2, jobInstances.size());

		for (JobInstance instance : jobInstances) {
			assertTrue(instance.getJobName().contains("Job"));
		}

		jobInstances = dao.getJobInstances("Job*", 0, 2);
		assertTrue(jobInstances.isEmpty());
	}

	@Transactional
	@Test
	void testDeleteJobInstance() {
		// given
		JobInstance jobInstance = dao.createJobInstance("someTestInstance", new JobParameters());

		// when
		dao.deleteJobInstance(jobInstance);

		// then
		Assertions.assertNull(dao.getJobInstance(jobInstance.getId()));
	}

	@Test
	void testDefaultJobKeyGeneratorIsUsed() {
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(jobInstanceDao,
				"jobKeyGenerator");
		Assertions.assertEquals(DefaultJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

}
