/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.StepContext;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class MessageOrientedStep extends AbstractStep {

	/**
	 * Key in execution context for flag to say we are waiting.
	 */
	public static final String WAITING = MessageOrientedStep.class.getName() + ".WAITING";

	private MessageChannel outputChannel;

	private PollableChannel source;

	private static int MINUTE = 1000 * 60;

	private long executionTimeout = 30*MINUTE ;

	private long pollingInterval = 5;

	/**
	 * Public setter for the execution timeout in minutes. Defaults to 30.
	 * @param executionTimeoutMinutes the timeout to set
	 */
	public void setExecutionTimeoutMinutes(int executionTimeoutMinutes) {
		this.executionTimeout = executionTimeoutMinutes * MINUTE;
	}
	
	/**
	 * Public setter for the execution timeout in milliseconds.  Defaults to 30 minutes.
	 * @param executionTimeout
	 */
	public void setExecutionTimeout(long executionTimeout) {
		this.executionTimeout = executionTimeout;
	}

	/**
	 * Public setter for the polling interval in milliseconds while waiting for
	 * replies signalling the end of the step.  Defaults to 5.
	 * @param pollingInterval the polling interval to set
	 */
	public void setPollingInterval(long pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Public setter for the target.
	 * @param outputChannel the target to set
	 */
	@Required
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Public setter for the source.
	 * @param source the source to set
	 */
	@Required
	public void setInputChannel(PollableChannel source) {
		this.source = source;
	}

	/**
	 * @see AbstractStep#execute(StepExecution)
	 */
	@Override
	public ExitStatus doExecute(StepContext stepContext) throws JobInterruptedException,
			UnexpectedJobExecutionException {

		StepExecution stepExecution = stepContext.getStepExecution();
		JobExecutionRequest request = new JobExecutionRequest(stepExecution .getJobExecution());

		ExecutionContext executionContext = stepExecution.getExecutionContext();

		if (executionContext.containsKey(WAITING)) {
			// restart scenario: we are still waiting for a response
			waitForReply(request.getJobId());
		}
		else {
			executionContext.putString(WAITING, "true");
			// TODO: need these two lines to be atomic
			getJobRepository().update(stepExecution);
			outputChannel.send(new GenericMessage<JobExecutionRequest>(request));
			waitForReply(request.getJobId());
		}

		return ExitStatus.FINISHED;

	}

	/**
	 * @param expectedJobId
	 */
	private void waitForReply(Long expectedJobId) {
		long timeout = pollingInterval;
		long maxCount = executionTimeout / timeout;
		long count = 0;

		while (count++ < maxCount) {

			// TODO: timeout?
			@SuppressWarnings("unchecked")
			Message<JobExecutionRequest> message = (Message<JobExecutionRequest>) source.receive(timeout);

			if (message != null) {

				JobExecutionRequest payload = message.getPayload();
				Long jobInstanceId = payload.getJobId();
				Assert.state(jobInstanceId != null, "Message did not contain job instance id.");
				Assert.state(jobInstanceId.equals(expectedJobId), "Message contained wrong job instance id ["
						+ jobInstanceId + "] should have been [" + expectedJobId + "].");

				if (payload.getStatus() == BatchStatus.COMPLETED) {
					// One of the steps decided we were finished
					// TODO: wait for all the other steps that might be
					// executing concurrently?
					// TODO: maybe *any* reply on this channel should
					// mean the end of the step?
					break;
				}

				if (payload.hasErrors()) {
					rethrow(payload.getLastThrowable());
				}

			}
		}

		if (count >= maxCount) {
			throw new StepExecutionTimeoutException("Timed out waiting for steps to execute.");
		}
	}

	/**
	 * @param lastThrowable
	 */
	private static void rethrow(Throwable t) throws RuntimeException {
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		if (t instanceof Exception) {
			throw new UnexpectedJobExecutionException("Unexpected checked exception thrown by step.", t);
		}
		throw (Error) t;
	}

}
