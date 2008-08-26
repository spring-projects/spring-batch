package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.springframework.batch.sample.tasklet.DummyMessageReceivingStepHandler;
import org.springframework.batch.sample.tasklet.DummyMessageSendingStepHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class JobExecutionContextSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	@Autowired
	private DummyMessageSendingStepHandler sender;

	@Autowired
	private DummyMessageReceivingStepHandler receiver;

	protected void validatePostConditions() throws Exception {
		assertEquals(sender.getMessage(), receiver.getReceivedMessage());
	}

}
