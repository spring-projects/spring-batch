package org.springframework.batch.sample;

import org.springframework.batch.sample.tasklet.DummyMessageReceivingTasklet;
import org.springframework.batch.sample.tasklet.DummyMessageSendingTasklet;

public class JobExecutionContextSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private DummyMessageSendingTasklet sender;

	private DummyMessageReceivingTasklet receiver;

	protected void validatePostConditions() throws Exception {
		assertEquals(sender.getMessage(), receiver.getReceivedMessage());
	}

	// auto-injection setter
	public void setSender(DummyMessageSendingTasklet sender) {
		this.sender = sender;
	}

	// auto-injection setter
	public void setReceiver(DummyMessageReceivingTasklet receiver) {
		this.receiver = receiver;
	}

}
