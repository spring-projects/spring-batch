package org.springframework.batch.execution.repository.dao;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.batch.execution.job.JobSupport;

public class MapJobInstanceDaoTests extends TestCase {

	private JobInstanceDao dao = new MapJobInstanceDao();

	private Job fooJob = new JobSupport("foo");

	private JobParameters fooParams = new JobParametersBuilder().addString("fooKey", "fooValue").toJobParameters();

	protected void setUp() throws Exception {
		MapJobInstanceDao.clear();
	}

	protected void tearDown() throws Exception {
		MapJobInstanceDao.clear();
	}

	/**
	 * Create and retrieve a job instance.
	 */
	public void testCreateAndRetrieve() throws Exception {

		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJob());
		assertEquals(fooParams, fooInstance.getJobParameters());

		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		assertEquals(fooInstance, retrievedInstance);
	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
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
}
