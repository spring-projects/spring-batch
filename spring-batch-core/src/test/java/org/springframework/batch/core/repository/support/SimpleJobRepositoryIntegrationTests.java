package org.springframework.batch.core.repository.support;

import java.util.Date;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.AbstractJobDaoTests;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 * 
 * @author Robert Kasanicky
 */
public class SimpleJobRepositoryIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(AbstractJobDaoTests.class, "sql-dao-test.xml") };
	}

	private SimpleJobRepository jobRepository;

	private JobSupport job = new JobSupport("SimpleJobRepositoryIntegrationTestsJob");

	private JobParameters jobParameters = new JobParameters();

	public void setJobRepository(SimpleJobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Create two job executions for same job+parameters tuple. Check both
	 * executions belong to the same job instance and job.
	 */
	public void testCreateAndFind() throws Exception {

		job.setRestartable(true);

		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("stringKey", "stringValue").addLong("longKey", 1L).addDouble("doubleKey", 1.1).addDate("dateKey", new Date(1L));
		JobParameters jobParams = builder.toJobParameters();

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParams);
		firstExecution.setStartTime(new Date());

		assertEquals(job.getName(), firstExecution.getJobInstance().getJobName());

		jobRepository.updateJobExecution(firstExecution);
		firstExecution.setEndTime(new Date());
		jobRepository.updateJobExecution(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job, jobParams);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/**
	 * Create two job executions for same job+parameters tuple. Check both
	 * executions belong to the same job instance and job.
	 */
	public void testCreateAndFindWithNoStartDate() throws Exception {
		job.setRestartable(true);

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParameters);
		firstExecution.setStartTime(new Date(0));
		firstExecution.setEndTime(new Date(1));
		jobRepository.updateJobExecution(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job, jobParameters);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/**
	 * Non-restartable JobInstance can be run only once - attempt to run
	 * existing non-restartable JobInstance causes error.
	 */
	public void testRunNonRestartableJobInstanceTwice() throws Exception {
		job.setRestartable(false);

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParameters);
		jobRepository.updateJobExecution(firstExecution);

		try {
			jobRepository.createJobExecution(job, jobParameters);
			fail();
		}
		catch (JobRestartException e) {
			// expected
		}
	}

	/**
	 * Save multiple StepExecutions for the same step and check the returned
	 * count and last execution are correct.
	 */
	public void testGetStepExecutionCountAndLastStepExecution() throws Exception {
		job.setRestartable(true);
		StepSupport step = new StepSupport("restartedStep");

		// first execution
		JobExecution firstJobExec = jobRepository.createJobExecution(job, jobParameters);
		StepExecution firstStepExec = new StepExecution(step.getName(), firstJobExec);
		jobRepository.updateJobExecution(firstJobExec);
		jobRepository.save(firstStepExec);

		assertEquals(1, jobRepository.getStepExecutionCount(firstJobExec.getJobInstance(), step));
		assertEquals(firstStepExec, jobRepository.getLastStepExecution(firstJobExec.getJobInstance(), step));

		// first execution failed
		firstJobExec.setStartTime(new Date(4));
		firstStepExec.setStartTime(new Date(5));
		firstStepExec.setStatus(BatchStatus.FAILED);
		firstStepExec.setEndTime(new Date(6));
		jobRepository.update(firstStepExec);
		firstJobExec.setStatus(BatchStatus.FAILED);
		firstJobExec.setEndTime(new Date(7));
		jobRepository.updateJobExecution(firstJobExec);

		// second execution
		JobExecution secondJobExec = jobRepository.createJobExecution(job, jobParameters);
		StepExecution secondStepExec = new StepExecution(step.getName(), secondJobExec);
		jobRepository.updateJobExecution(secondJobExec);
		jobRepository.save(secondStepExec);

		assertEquals(2, jobRepository.getStepExecutionCount(secondJobExec.getJobInstance(), step));
		assertEquals(secondStepExec, jobRepository.getLastStepExecution(secondJobExec.getJobInstance(), step));
	}

	/**
	 * Save execution context and retrieve it.
	 */
	public void testSaveExecutionContext() throws Exception {
		ExecutionContext ctx = new ExecutionContext() {
			{
				putLong("crashedPosition", 7);
			}
		};
		JobExecution jobExec = jobRepository.createJobExecution(job, jobParameters);
		jobExec.setStartTime(new Date(0));
		jobExec.setExecutionContext(ctx);
		Step step = new StepSupport("step1");
		StepExecution stepExec = new StepExecution(step.getName(), jobExec);
		stepExec.setExecutionContext(ctx);

		jobRepository.save(stepExec);
		jobRepository.saveOrUpdateExecutionContext(stepExec);

		StepExecution retrievedStepExec = jobRepository.getLastStepExecution(jobExec.getJobInstance(), step);
		assertEquals(stepExec, retrievedStepExec);
		assertEquals(ctx, retrievedStepExec.getExecutionContext());
		
//		JobExecution retrievedJobExec = jobRepository.getLastJobExecution(jobExec.getJobInstance());
//		assertEquals(jobExec, retrievedJobExec);
//		assertEquals(ctx, retrievedJobExec.getExecutionContext());
	}

	/**
	 * If JobExecution is already running, exception will be thrown in attempt
	 * to create new execution.
	 */
	public void testOnlyOneJobExecutionAllowedRunning() throws Exception {
		job.setRestartable(true);
		jobRepository.createJobExecution(job, jobParameters);
		
		try {
			jobRepository.createJobExecution(job, jobParameters);
			fail();
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
	}
	
}