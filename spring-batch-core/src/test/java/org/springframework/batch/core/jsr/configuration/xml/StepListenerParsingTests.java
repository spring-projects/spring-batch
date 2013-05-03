package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration({"StepListenerParsingTests-context.xml", "jsr-base-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class StepListenerParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public StepListener stepListener;

	@Test
	public void test() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
		assertEquals(2, stepListener.countBeforeStep);
		assertEquals(2, stepListener.countAfterStep);
	}

	public static class StepListener implements StepExecutionListener {
		protected int countBeforeStep = 0;
		protected int countAfterStep = 0;

		@Override
		public void beforeStep(StepExecution stepExecution) {
			countBeforeStep++;
		}

		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			countAfterStep++;
			return null;
		}
	}
}
