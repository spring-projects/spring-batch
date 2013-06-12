package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration({"JobListenerParsingTests-context.xml", "jsr-base-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class JobListenerParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public JobListener listener;

	@Test
	public void test() throws Exception {
		assertNotNull(job);
		assertEquals("job1", job.getName());

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
		assertEquals(1, listener.countAfterJob);
		assertEquals(1, listener.countBeforeJob);
	}

	public static class JobListener implements JobExecutionListener {

		protected int countBeforeJob = 0;
		protected int countAfterJob = 0;

		@Override
		public void beforeJob(JobExecution jobExecution) {
			countBeforeJob++;
		}

		@Override
		public void afterJob(JobExecution jobExecution) {
			countAfterJob++;
		}
	}
}
