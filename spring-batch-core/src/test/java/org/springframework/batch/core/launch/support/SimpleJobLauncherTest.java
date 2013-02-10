package org.springframework.batch.core.launch.support;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * 
 * @author Will Schipp
 *
 */
public class SimpleJobLauncherTest {

	private SimpleJobLauncher jobLauncher;
	
	/**
	 * Test to support BATCH-1770 -> throw in parent thread JobRestartException when 
	 * a stepExecution is UNKNOWN
	 */
	@Test
	public void testRunStepStatusUNKNOWN() {
		//try and restart a job where the step execution is UNKNOWN 
		//setup
		String jobName = "test_job";
		JobRepository jobRepository = mock(JobRepository.class);
		JobParameters parameters = new JobParametersBuilder().addLong("runtime", System.currentTimeMillis()).toJobParameters();
		JobExecution jobExecution = mock(JobExecution.class);
		Job job = mock(Job.class);
		JobParametersValidator validator = mock(JobParametersValidator.class);
		StepExecution stepExecution = mock(StepExecution.class);
		
		when(job.getName()).thenReturn(jobName);
		when(job.isRestartable()).thenReturn(true);		
		when(job.getJobParametersValidator()).thenReturn(validator);
		when(jobRepository.getLastJobExecution(jobName, parameters)).thenReturn(jobExecution);
		when(stepExecution.getStatus()).thenReturn(BatchStatus.UNKNOWN);
		when(jobExecution.getStepExecutions()).thenReturn(Arrays.asList(stepExecution));
		
		//setup launcher
		jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		
		//run
		try {
			jobLauncher.run(job, parameters);
		} catch (JobExecutionAlreadyRunningException e) {
			fail(e.toString());
		} catch (JobRestartException e) {
			//expect exception
			assertTrue(e.getMessage().contains("is of status UNKNOWN"));
		} catch (JobInstanceAlreadyCompleteException e) {
			fail(e.toString());
		} catch (JobParametersInvalidException e) {
			fail(e.toString());
		}

	}
}
