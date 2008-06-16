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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "/job-execution-context.xml" })
public class JobLaunchingMessageHandlerTests extends AbstractJUnit4SpringContextTests {
	
	JobLaunchingMessageHandler messageHandler;

	StubJobLauncher jobLauncher;

	//	@Autowired
	//	@Qualifier("jobs") TODO: Qualifier seems to be broken here why ?????
	public AbstractMessageChannel jobsChannel;
	
	@Autowired
	public MessageBus messageBus;

	@Before
	public void setUp() {
		jobLauncher = new StubJobLauncher();
		messageHandler = new JobLaunchingMessageHandler(jobLauncher);
		jobsChannel = (AbstractMessageChannel) applicationContext.getBean("jobs");
	}
	
	@Test
	public void testSimpleDelivery() throws Exception{
		messageHandler.launch(new JobExecutionRequest(new JobSupport("testjob"), null));
		
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
