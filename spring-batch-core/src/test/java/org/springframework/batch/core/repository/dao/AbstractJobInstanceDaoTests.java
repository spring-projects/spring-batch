/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractJobInstanceDaoTests {

	private static final long DATE = 777;

	protected JobInstanceDao dao;

	private final String fooJob = "foo";

	private final JobParameters fooParams = new JobParametersBuilder().addString("stringKey", "stringValue")
		.addLong("longKey", Long.MAX_VALUE)
		.addDouble("doubleKey", Double.MAX_VALUE)
		.addDate("dateKey", new Date(DATE))
		.toJobParameters();

	protected abstract JobInstanceDao getJobInstanceDao();

	@BeforeEach
	void onSetUp() {
		dao = getJobInstanceDao();
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional
	@Test
	void testCreateAndRetrieve() {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJobName());

		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional
	@Test
	void testCreateAndGetById() {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJobName());

		JobInstance retrievedInstance = dao.getJobInstance(fooInstance.getId());
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional
	@Test
	void testGetMissingById() {

		JobInstance retrievedInstance = dao.getJobInstance(1111111L);
		assertNull(retrievedInstance);

	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional
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
	@Transactional
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
		assertEquals(Integer.valueOf(0), jobInstances.get(0).getVersion());
		assertEquals(Integer.valueOf(0), jobInstances.get(1).getVersion());

		assertTrue(jobInstances.get(0).getId() > jobInstances.get(1).getId(),
				"Last instance should be first on the list");

	}

	@Transactional
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

	@Transactional
	@Test
	void testGetLastInstanceWhenNoInstance() {
		JobInstance lastJobInstance = dao.getLastJobInstance("NonExistingJob");
		assertNull(lastJobInstance);
	}

	/**
	 * Create and retrieve a job instance.
	 */
	@Transactional
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
			assertEquals(Integer.valueOf(0), returnedInstance.getVersion());

			// checks the correct instances are returned and the order is descending
			// assertEquals(instanceCount - startIndex - i ,
			// returnedInstance.getJobParameters().getLong(paramKey));
		}

	}

	/**
	 * Create and retrieve a job instance.
	 */
	@Transactional
	@Test
	void testGetLastInstancesPastEnd() {

		testCreateAndRetrieve();

		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());

		// we need two instances of the same job to check ordering
		dao.createJobInstance(fooJob, new JobParameters());

		List<JobInstance> jobInstances = dao.getJobInstances(fooJob, 4, 2);
		assertEquals(0, jobInstances.size());

	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
	@Transactional
	@Test
	void testCreateDuplicateInstance() {

		dao.createJobInstance(fooJob, fooParams);

		assertThrows(IllegalStateException.class, () -> dao.createJobInstance(fooJob, fooParams));
	}

	@Transactional
	@Test
	void testCreationAddsVersion() {

		JobInstance jobInstance = new JobInstance(1L, "testVersionAndId");

		assertNull(jobInstance.getVersion());

		jobInstance = dao.createJobInstance("testVersion", new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}

	public void testGetJobInstanceByExecutionId() {
		// TODO: test this (or maybe the method isn't needed or has wrong signature)
	}

}
