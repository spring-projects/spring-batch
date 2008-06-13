package org.springframework.integration.batch.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.batch.JobSupport;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = { "/job-execution-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLaunchingPostReceiveChannelAdapterTests extends AbstractJUnit4SpringContextTests {

	JobLaunchingPostReceiveChannelInterceptor interceptor;

	StubJobLauncher jobLauncher;

	JobSupport job;

//	@Autowired
	//	@Qualifier("jobs") TODO: Qualifier seems to be broken here why ?????
	public AbstractMessageChannel jobsChannel;

	@Before
	public void setUp() {
		job = new JobSupport(getClass().getName());
		jobLauncher = new StubJobLauncher();
		interceptor = new JobLaunchingPostReceiveChannelInterceptor(job, jobLauncher);
		jobsChannel = (AbstractMessageChannel) applicationContext.getBean("jobs");
		jobsChannel.addInterceptor(interceptor);
	}

	@Test
	public void testJobPassedToLauncherCalled() {
		StringMessage message = new StringMessage("test payload");
		jobsChannel.send(message);
		assertTrue("Job launcher called before recevie", (jobLauncher.jobs.size() == 0));
		jobsChannel.receive();
		assertEquals(job, jobLauncher.jobs.get(0));
	}

	@Test
	public void testMessagePropertiesPassedAsJobParameters() {
		StringMessage message = new StringMessage("test payload");
		message.getHeader().setProperty("testOne", "a");
		message.getHeader().setProperty("testTwo", "b");
		jobsChannel.send(message);
		jobsChannel.receive();
		JobParameters parameters = jobLauncher.parameters.get(0);
		assertEquals("a", parameters.getString("testOne"));
		assertEquals("b", parameters.getString("testTwo"));

	}

	private static class StubJobLauncher implements JobLauncher {

		List<Job> jobs = new ArrayList<Job>();
		
		List<JobParameters> parameters = new ArrayList<JobParameters>();

		
		public JobExecution run(Job job, JobParameters jobParameters){
			jobs.add(job);
			parameters.add(jobParameters);
			return null;
		}

	}

}
