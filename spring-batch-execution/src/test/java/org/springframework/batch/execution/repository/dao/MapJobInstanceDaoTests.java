package org.springframework.batch.execution.repository.dao;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.batch.execution.job.JobSupport;

public class MapJobInstanceDaoTests extends TestCase {

	JobInstanceDao dao = new MapJobInstanceDao();
	
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
		Job fooJob = new JobSupport("foo");
		JobParameters fooParams = new JobParametersBuilder().addString("fooKey", "fooValue").toJobParameters();
		
		JobInstance fooInstance = dao.createJobInstance(fooJob, fooParams);
		assertNotNull(fooInstance.getId());
		assertEquals(fooJob, fooInstance.getJob());
		assertEquals(fooParams, fooInstance.getJobParameters());
		
		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		assertEquals(fooInstance, retrievedInstance);
	}
}
