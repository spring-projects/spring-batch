package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.springframework.batch.sample.tasklet.DummyMessageReceivingTasklet;
import org.springframework.batch.sample.tasklet.DummyMessageSendingTasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class JobExecutionContextSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	@Autowired
	private DummyMessageSendingTasklet sender;

	@Autowired
	private DummyMessageReceivingTasklet receiver;

	protected void validatePostConditions() throws Exception {
		assertEquals(sender.getMessage(), receiver.getReceivedMessage());
	}

}
