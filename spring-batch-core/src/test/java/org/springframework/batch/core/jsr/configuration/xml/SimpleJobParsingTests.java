package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.batch.api.Batchlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleJobParsingTests {

	@Autowired
	public Job job;

	@Autowired
	@Qualifier("step1")
	public Step step1;

	@Autowired
	@Qualifier("step2")
	public Step step2;

	@Autowired
	@Qualifier("step3")
	public Step step3;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public Batchlet batchlet;

	@Test
	public void test() throws Exception {
		assertNotNull(job);
		assertEquals("job1", job.getName());
		assertNotNull(step1);
		assertEquals("step1", step1.getName());
		assertNotNull(step2);
		assertEquals("step2", step2.getName());
		assertNotNull(step3);
		assertEquals("step3", step3.getName());
		assertNotNull(batchlet);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
	}
}
