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
package org.springframework.batch.core.repository.support;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.DefaultJobKeyGenerator;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobKeyGenerator;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
class MongoJobInstanceDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private JobInstanceDao dao;

	private final JobParameters fooParams = new JobParametersBuilder().addString("stringKey", "stringValue")
		.addLong("longKey", Long.MAX_VALUE)
		.addDouble("doubleKey", Double.MAX_VALUE)
		.addDate("dateKey", new Date(DATE))
		.toJobParameters();

	private static final long DATE = 777;

	private final String fooJob = "foo";

	@Test
	void testFindJobInstanceByExecution(@Autowired JobExecutionDao jobExecutionDao) {

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = dao.createJobInstance("testInstance", jobParameters);
		JobExecution jobExecution = jobExecutionDao.createJobExecution(jobInstance, jobParameters);

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

	@Disabled("Not supported yet")
	@Test
	void testJobInstanceWildcard() {
		dao.createJobInstance("anotherJob", new JobParameters());
		dao.createJobInstance("someJob", new JobParameters());

		List<JobInstance> jobInstances = dao.getJobInstances("*Job", 0, 2);
		assertEquals(2, jobInstances.size());

		for (JobInstance instance : jobInstances) {
			assertTrue(instance.getJobName().contains("Job"));
		}

		jobInstances = dao.getJobInstances("Job*", 0, 2);
		assertTrue(jobInstances.isEmpty());
	}

	@Test
	void testDeleteJobInstance() {
		// given
		JobInstance jobInstance = dao.createJobInstance("someTestInstance", new JobParameters());

		// when
		dao.deleteJobInstance(jobInstance);

		// then
		assertNull(dao.getJobInstance(jobInstance.getId()));
	}

	@Test
	void testDefaultJobKeyGeneratorIsUsed() {
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(dao, "jobKeyGenerator");
		assertNotNull(jobKeyGenerator);
		assertEquals(DefaultJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

	/*
	 * Create and retrieve a job instance.
	 */

	@Test
	void testCreateAndRetrieve() {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertEquals(fooJob, fooInstance.getJobName());

		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		assertNotNull(retrievedInstance);
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
	}

	/*
	 * Create and retrieve a job instance.
	 */

	@Test
	void testCreateAndGetById() {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertEquals(fooJob, fooInstance.getJobName());

		JobInstance retrievedInstance = dao.getJobInstance(fooInstance.getId());
		assertNotNull(retrievedInstance);
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
	}

	/*
	 * Create and retrieve a job instance.
	 */

	@Test
	void testGetMissingById() {

		JobInstance retrievedInstance = dao.getJobInstance(1111111L);
		assertNull(retrievedInstance);

	}

	/*
	 * Create and retrieve a job instance.
	 */

	@Test
	void testGetJobNames() {

		testCreateAndRetrieve();
		List<String> jobNames = dao.getJobNames();
		assertFalse(jobNames.isEmpty());
		assertTrue(jobNames.contains(fooJob));

	}

	/**
	 * Create and retrieve a job instance.
	 */

	@Test
	void testGetLastInstances() {

		testCreateAndRetrieve();

		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());

		// we need two instances of the same job to check ordering
		dao.createJobInstance(fooJob, new JobParameters());

		List<JobInstance> jobInstances = dao.getJobInstances(fooJob, 0, 2);
		assertEquals(2, jobInstances.size());
		assertEquals(fooJob, jobInstances.get(0).getJobName());
		assertEquals(fooJob, jobInstances.get(1).getJobName());
		// assertEquals(Integer.valueOf(0), jobInstances.get(0).getVersion());
		// assertEquals(Integer.valueOf(0), jobInstances.get(1).getVersion());

		assertTrue(jobInstances.get(0).getId() > jobInstances.get(1).getId(),
				"Last instance should be first on the list");

	}

	@Test
	void testGetLastInstance() {
		testCreateAndRetrieve();

		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());

		// we need two instances of the same job to check ordering
		dao.createJobInstance(fooJob, new JobParameters());

		List<JobInstance> jobInstances = dao.getJobInstances(fooJob, 0, 2);
		assertEquals(2, jobInstances.size());
		JobInstance lastJobInstance = dao.getLastJobInstance(fooJob);
		assertNotNull(lastJobInstance);
		assertEquals(fooJob, lastJobInstance.getJobName());
		assertEquals(jobInstances.get(0), lastJobInstance, "Last instance should be first on the list");
	}

	@Test
	void testGetLastInstanceWhenNoInstance() {
		JobInstance lastJobInstance = dao.getLastJobInstance("NonExistingJob");
		assertNull(lastJobInstance);
	}

	/**
	 * Create and retrieve a job instance.
	 */

	@Test
	void testGetLastInstancesPaged() {

		testCreateAndRetrieve();

		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());

		// we need multiple instances of the same job to check ordering
		String multiInstanceJob = "multiInstanceJob";
		String paramKey = "myID";
		int instanceCount = 6;
		for (int i = 1; i <= instanceCount; i++) {
			JobParameters params = new JobParametersBuilder().addLong(paramKey, (long) i).toJobParameters();
			dao.createJobInstance(multiInstanceJob, params);
		}

		int startIndex = 3;
		int queryCount = 2;
		List<JobInstance> jobInstances = dao.getJobInstances(multiInstanceJob, startIndex, queryCount);

		assertEquals(queryCount, jobInstances.size());

		for (int i = 0; i < queryCount; i++) {
			JobInstance returnedInstance = jobInstances.get(i);
			assertEquals(multiInstanceJob, returnedInstance.getJobName());
			// assertEquals(Integer.valueOf(0), returnedInstance.getVersion());

			// checks the correct instances are returned and the order is descending
			// assertEquals(instanceCount - startIndex - i ,
			// returnedInstance.getJobParameters().getLong(paramKey));
		}

	}

	/**
	 * Create and retrieve a job instance.
	 */

	@Test
	void testGetLastInstancesPastEnd() {

		testCreateAndRetrieve();

		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());

		// we need two instances of the same job to check ordering
		dao.createJobInstance(fooJob, new JobParameters());

		assertEquals(1, dao.getJobInstances(fooJob, 0, 1).size());
		assertEquals(2, dao.getJobInstances(fooJob, 0, 2).size());
		assertEquals(2, dao.getJobInstances(fooJob, 0, 3).size());
		assertEquals(1, dao.getJobInstances(fooJob, 1, 3).size());
		assertEquals(0, dao.getJobInstances(fooJob, 0, 0).size());
		assertEquals(0, dao.getJobInstances(fooJob, 4, 2).size());

	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */

	@Test
	void testCreateDuplicateInstance() {

		dao.createJobInstance(fooJob, fooParams);

		assertThrows(IllegalStateException.class, () -> dao.createJobInstance(fooJob, fooParams));
	}

	@Disabled("Version is not persisted")
	@Test
	void testCreationAddsVersion() {

		JobInstance jobInstance = new JobInstance(1L, "testVersionAndId");

		assertNull(jobInstance.getVersion());

		jobInstance = dao.createJobInstance("testVersion", new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}

}