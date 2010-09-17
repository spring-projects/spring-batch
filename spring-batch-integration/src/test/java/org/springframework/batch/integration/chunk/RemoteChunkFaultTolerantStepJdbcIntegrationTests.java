package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
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
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoteChunkFaultTolerantStepJdbcIntegrationTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;
	
	@Autowired
	private PollableChannel replies;
	
	// @Autowired
	// private DataSource dataSource;
	
	@Before
	public void drain() {
		Message<?> message = replies.receive(100L);
		while (message!=null) {
			// System.err.println(message);
			message = replies.receive(100L);
		}
	}

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
		// System.err.println(new SimpleJdbcTemplate(dataSource).queryForList("SELECT * FROM INT_MESSAGE_GROUP"));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(7, stepExecution.getWriteCount());
		// The whole chunk gets skipped...
		assertEquals(2, stepExecution.getWriteSkipCount());
	}
}
