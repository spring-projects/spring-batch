package org.springframework.batch.core.job;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;

/**
 * Test suite for various failure scenarios during job processing.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class SimpleJobFailureTests {

	private SimpleJob job = new SimpleJob("job");

	private JobExecution execution;

	@Before
	public void init() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		job.setJobRepository(jobRepository);
		execution = jobRepository.createJobExecution("job", new JobParameters());
	}

	@Test
	public void testStepFailure() throws Exception {
		job.setSteps(Arrays.<Step> asList(new StepSupport("step")));
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	public void testStepStatusUnknown() throws Exception {
		job.setSteps(Arrays.<Step> asList(new StepSupport("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				// This is what happens if the repository meta-data cannot be updated
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				stepExecution.setTerminateOnly();
			}
		}, new StepSupport("step2")));
		job.execute(execution);
		assertEquals(BatchStatus.UNKNOWN, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
	}

}
