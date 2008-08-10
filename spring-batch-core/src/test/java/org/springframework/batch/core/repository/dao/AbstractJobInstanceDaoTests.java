package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractJobInstanceDaoTests extends AbstractTransactionalJUnit4SpringContextTests {

	private static final long DATE = 777;

	private JobInstanceDao dao = new MapJobInstanceDao();

	private String fooJob = "foo";

	private JobParameters fooParams = new JobParametersBuilder().addString("stringKey", "stringValue").addLong(
			"longKey", Long.MAX_VALUE).addDouble("doubleKey", Double.MAX_VALUE).addDate(
			"dateKey", new Date(DATE)).toJobParameters();

	protected abstract JobInstanceDao getJobInstanceDao();

	@Before
	public void onSetUp() throws Exception {
		dao = getJobInstanceDao();
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional @Test
	public void testCreateAndRetrieve() throws Exception {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJobName());
		assertEquals(fooParams, fooInstance.getJobParameters());

		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		JobParameters retrievedParams = retrievedInstance.getJobParameters();
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
		assertEquals(fooParams, retrievedParams);
		
		assertEquals(Long.MAX_VALUE, retrievedParams.getLong("longKey"));
		assertEquals(Double.MAX_VALUE, retrievedParams.getDouble("doubleKey"), 0.001);
		assertEquals("stringValue", retrievedParams.getString("stringKey"));
		assertEquals(new Date(DATE), retrievedParams.getDate("dateKey"));
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional @Test
	public void testCreateAndGetById() throws Exception {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJobName());
		assertEquals(fooParams, fooInstance.getJobParameters());

		JobInstance retrievedInstance = dao.getJobInstance(fooInstance.getId());
		JobParameters retrievedParams = retrievedInstance.getJobParameters();
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob, retrievedInstance.getJobName());
		assertEquals(fooParams, retrievedParams);
		
		assertEquals(Long.MAX_VALUE, retrievedParams.getLong("longKey"));
		assertEquals(Double.MAX_VALUE, retrievedParams.getDouble("doubleKey"), 0.001);
		assertEquals("stringValue", retrievedParams.getString("stringKey"));
		assertEquals(new Date(DATE), retrievedParams.getDate("dateKey"));
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional @Test
	public void testGetJobNames() throws Exception {
		
		testCreateAndRetrieve();
		List<String> jobNames = dao.getJobNames();
		assertFalse(jobNames.isEmpty());
		assertTrue(jobNames.contains(fooJob));
		
	}

	/*
	 * Create and retrieve a job instance.
	 */
	@Transactional @Test
	public void testGetLastInstances() throws Exception {
		
		testCreateAndRetrieve();
		List<JobInstance> jobInstances = dao.getLastJobInstances(fooJob, 1);
		assertEquals(1, jobInstances.size());
		assertEquals(fooJob, jobInstances.get(0).getJobName());
		assertEquals(Integer.valueOf(0), jobInstances.get(0).getVersion());
		
	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
	@Transactional @Test
	public void testCreateDuplicateInstance() {

		dao.createJobInstance(fooJob, fooParams);

		try {
			dao.createJobInstance(fooJob, fooParams);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	@Transactional @Test
	public void testCreationAddsVersion() {

		JobInstance jobInstance = new JobInstance((long) 1, new JobParameters(), "testVersionAndId");

		assertNull(jobInstance.getVersion());

		jobInstance = dao.createJobInstance("testVersion", new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}

}
