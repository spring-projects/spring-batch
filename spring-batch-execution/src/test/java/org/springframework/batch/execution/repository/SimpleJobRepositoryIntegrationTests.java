package org.springframework.batch.execution.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 * 
 * @author Robert Kasanicky
 */
public class SimpleJobRepositoryIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "dao/sql-dao-test.xml") };
	}

	private SimpleJobRepository jobRepository;

	public void setJobRepository(SimpleJobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Create two job executions for same job+parameters tuple. Check both
	 * executions belong to the same job instance and job.
	 */
	public void testCreateAndFind() throws Exception {

		JobSupport job = new JobSupport("testJob");
		job.setRestartable(true);

		Map stringParams = new HashMap() {
			{
				put("stringKey", "stringValue");
			}
		};
		Map longParams = new HashMap() {
			{
				put("longKey", new Long(1));
			}
		};
		Map doubleParams = new HashMap() {
			{
				put("doubleKey", new Double(1.1));
			}
		};
		Map dateParams = new HashMap() {
			{
				put("dateKey", new Date(1));
			}
		};
		JobParameters jobParams = new JobParameters(stringParams, longParams, doubleParams, dateParams);

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParams);
		firstExecution.setStartTime(new Date());

		assertEquals(job, firstExecution.getJobInstance().getJob());

		jobRepository.saveOrUpdate(firstExecution);
		firstExecution.setEndTime(new Date());
		jobRepository.saveOrUpdate(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job, jobParams);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job, secondExecution.getJobInstance().getJob());
	}

	/**
	 * Create two job executions for same job+parameters tuple. Check both
	 * executions belong to the same job instance and job.
	 */
	public void testCreateAndFindWithNoStartDate() throws Exception {

		JobSupport job = new JobSupport("testJob");
		job.setRestartable(true);
		JobParameters jobParams = new JobParameters();

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParams);
		firstExecution.setEndTime(new Date());
		jobRepository.saveOrUpdate(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job, jobParams);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job, secondExecution.getJobInstance().getJob());
	}

	/**
	 * Non-restartable JobInstance can be run only once - attempt to run
	 * existing non-restartable JobInstance causes error.
	 */
	public void testRunNonRestartableJobInstanceTwice() throws Exception {
		JobSupport job = new JobSupport("nonRestartableJob");
		job.setRestartable(false);
		JobParameters jobParameters = new JobParameters();
		
		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParameters);
		jobRepository.saveOrUpdate(firstExecution);
		
		try {
			jobRepository.createJobExecution(job, jobParameters);
			fail();
		}
		catch (JobRestartException e) {
			// expected
		}
	}
}