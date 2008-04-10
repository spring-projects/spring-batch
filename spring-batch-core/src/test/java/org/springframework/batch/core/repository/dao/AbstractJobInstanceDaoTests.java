package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public abstract class AbstractJobInstanceDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	private JobInstanceDao dao = new MapJobInstanceDao();

	private Job fooJob = new JobSupport("foo");

	private JobParameters fooParams = new JobParametersBuilder().addString("fooKey", "fooValue").toJobParameters();

	protected abstract JobInstanceDao getJobInstanceDao();
	
	protected void onSetUp() throws Exception {
		dao = getJobInstanceDao();
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

	public void testCreationAddsVersion() {
		
		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("testVersionAndId"));
		
		assertNull(jobInstance.getVersion());

		jobInstance = dao.createJobInstance(new JobSupport("testVersion"), new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}
}
