package org.springframework.batch.sample.tasklet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.tasklet.StepHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.AttributeAccessor;

/**
 * Dummy tasklet that stores a message in the job execution context.
 */
public class DummyMessageSendingStepHandler extends StepExecutionListenerSupport implements StepHandler {

	private static final Log logger = LogFactory.getLog(DummyMessageSendingStepHandler.class);

	public static final String MESSAGE_KEY = "DummyMessageSendingStepHandler.MESSAGE";

	private String message = "Hello!";

	public ExitStatus afterStep(StepExecution stepExecution) {
		ExecutionContext ctx = stepExecution.getJobExecution().getExecutionContext();
		ctx.putString(MESSAGE_KEY, message);
		logger.info("Put message into context: " + message);
		return null;
	}
	
	public ExitStatus handle(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		return ExitStatus.FINISHED;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
