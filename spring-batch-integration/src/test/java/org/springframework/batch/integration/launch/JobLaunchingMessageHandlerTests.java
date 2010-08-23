package org.springframework.batch.integration.launch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.JobSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "/job-execution-context.xml" })
public class JobLaunchingMessageHandlerTests extends AbstractJUnit4SpringContextTests {
	
	JobLaunchRequestHandler messageHandler;

	StubJobLauncher jobLauncher;

	@Before
	public void setUp() {
		jobLauncher = new StubJobLauncher();
		messageHandler = new JobLaunchingMessageHandler(jobLauncher);
	}
	
	@Test
	public void testSimpleDelivery() throws Exception{
		messageHandler.launch(new JobLaunchRequest(new JobSupport("testjob"), null));
		
		assertEquals("Wrong job count", 1, jobLauncher.jobs.size());
		assertEquals("Wrong job name", jobLauncher.jobs.get(0).getName(), "testjob");
		
	}

	private static class StubJobLauncher implements JobLauncher {

		List<Job> jobs = new ArrayList<Job>();
		
		List<JobParameters> parameters = new ArrayList<JobParameters>();

		AtomicLong jobId = new AtomicLong();
		
		public JobExecution run(Job job, JobParameters jobParameters){
			jobs.add(job);
			parameters.add(jobParameters);
			return new JobExecution(new JobInstance(jobId.getAndIncrement(), jobParameters, job.getName()));
		}

	}

}
