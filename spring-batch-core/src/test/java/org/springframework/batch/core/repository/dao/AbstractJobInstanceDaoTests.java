package org.springframework.batch.core.repository.dao;

import java.util.Date;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public abstract class AbstractJobInstanceDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	private static final long DATE = 777;

	private JobInstanceDao dao = new MapJobInstanceDao();

	private Job fooJob = new JobSupport("foo");

	private JobParameters fooParams = new JobParametersBuilder().addString("stringKey", "stringValue").addLong(
			"longKey", new Long(Long.MAX_VALUE)).addDouble("doubleKey", new Double(Double.MAX_VALUE)).addDate(
			"dateKey", new Date(DATE)).toJobParameters();

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
		assertEquals(fooJob.getName(), fooInstance.getJobName());
		assertEquals(fooParams, fooInstance.getJobParameters());

		JobInstance retrievedInstance = dao.getJobInstance(fooJob, fooParams);
		JobParameters retrievedParams = retrievedInstance.getJobParameters();
		assertEquals(fooInstance, retrievedInstance);
		assertEquals(fooJob.getName(), retrievedInstance.getJobName());
		assertEquals(fooParams, retrievedParams);
		
		assertEquals(Long.MAX_VALUE, retrievedParams.getLong("longKey").longValue());
		assertEquals(Double.MAX_VALUE, retrievedParams.getDouble("doubleKey").doubleValue(), 0.001);
		assertEquals("stringValue", retrievedParams.getString("stringKey"));
		assertEquals(new Date(DATE), retrievedParams.getDate("dateKey"));
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

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "testVersionAndId");

		assertNull(jobInstance.getVersion());

		jobInstance = dao.createJobInstance(new JobSupport("testVersion"), new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}

}
