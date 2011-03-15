package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileSystemUtils;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RemoteChunkFaultTolerantStepJmsIntegrationTests {
	
	@BeforeClass
	public static void clear() {
		FileSystemUtils.deleteRecursively(new File("activemq-data"));
	}

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Test
	public void testFailedStep() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters(Collections.singletonMap("item.three",
				new JobParameter("unsupported"))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	public void testFailedStepOnError() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters(Collections.singletonMap("item.three",
				new JobParameter("error"))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	public void testSunnyDayFaultTolerant() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters(Collections.singletonMap("item.three",
				new JobParameter("3"))));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(9, stepExecution.getWriteCount());
	}

	@Test
	public void testSkipsInWriter() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParametersBuilder().addString("item.three", "fail")
				.addLong("run.id", 1L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(7, stepExecution.getWriteCount());
		assertEquals(2, stepExecution.getWriteSkipCount());
	}
}
