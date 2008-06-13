package org.springframework.batch.integration.launch;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLaunchingMessageHandlerIntegrationTests  {
	
	@Autowired @Qualifier("requests")
	private MessageChannel requestChannel;
	
	@Autowired @Qualifier("response")
	private MessageChannel responseChannel;

	@Before
	public void setUp(){
		requestChannel.purge(null);
		responseChannel.purge(null);
	}
	
	
	@Test @DirtiesContext @SuppressWarnings("unchecked")
	public void testNoReply(){
		requestChannel.send(new StringMessage("testJob"));
		Message<JobExecution> executionMessage = (Message<JobExecution>)responseChannel.receive(1000);
		
		assertNull("JobExecution message received when no return address set", executionMessage);
	}	

	
	@SuppressWarnings("unchecked")
	@Test @DirtiesContext
	public void testReply(){
		StringMessage trigger = new StringMessage("testJob");
		trigger.getHeader().setProperty("dontclash", "12");
		trigger.getHeader().setReturnAddress("response");
		requestChannel.send(trigger);
		Message<JobExecution> executionMessage = (Message<JobExecution>)responseChannel.receive(1000);

		assertNotNull("No response received", executionMessage);
		JobExecution execution = executionMessage.getPayload();
		assertNotNull("JobExectuion not returned", execution);
	}	

}
