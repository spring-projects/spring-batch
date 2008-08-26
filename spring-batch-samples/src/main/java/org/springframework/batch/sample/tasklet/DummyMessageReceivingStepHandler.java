package org.springframework.batch.sample.tasklet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.handler.StepHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.AttributeAccessor;

/**
 * Dummy tasklet that retrieves message from the job execution context.
 */
public class DummyMessageReceivingStepHandler extends StepExecutionListenerSupport implements StepHandler {

	private static final Log logger = LogFactory.getLog(DummyMessageReceivingStepHandler.class);

	private String receivedMessage = null;

	public void beforeStep(StepExecution stepExecution) {
		ExecutionContext ctx = stepExecution.getJobExecution().getExecutionContext();
		receivedMessage = ctx.getString(DummyMessageSendingStepHandler.MESSAGE_KEY);
		logger.info("Got message from context: " + receivedMessage);
	}

	public ExitStatus handle(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		return ExitStatus.FINISHED;
	}

	public String getReceivedMessage() {
		return receivedMessage;
	}

}
