package org.springframework.batch.core.test.timeout;

import static org.junit.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/timeoutJob.xml" })
public class TimeoutJobIntegrationTests {

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	@Qualifier("chunkTimeoutJob")
	private Job chunkTimeoutJob;
	
	@Autowired
	@Qualifier("taskletTimeoutJob")
	private Job taskletTimeoutJob;	

	@Test
	public void testChunkTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(chunkTimeoutJob, new JobParametersBuilder().addLong("id", System.currentTimeMillis())
				.toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	public void testTaskletTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(taskletTimeoutJob, new JobParametersBuilder().addLong("id", System.currentTimeMillis())
				.toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

}
