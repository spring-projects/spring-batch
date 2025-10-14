/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.remote;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.infrastructure.poller.DirectPoller;
import org.springframework.batch.infrastructure.poller.Poller;
import org.springframework.batch.integration.partition.StepExecutionRequest;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link org.springframework.batch.core.step.Step} implementation that delegates the
 * execution to a remote worker step through messaging.
 * <p>
 * The remote worker step must be listening to the same message channel to receive step
 * execution requests.
 * <p>
 * The step execution is created locally and sent to the remote worker step which will
 * update its status and context in the job repository. The {@link RemoteStep} will poll
 * the job repository to check for step completion.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0.0
 */
public class RemoteStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(RemoteStep.class.getName());

	private final String remoteStepName;

	private final MessagingTemplate messagingTemplate;

	private MessageChannel messageChannel;

	private long pollInterval = 10000;

	private long timeout = -1;

	/**
	 * Create a new {@link RemoteStep} instance.
	 * @param name the name of this step
	 * @param remoteStepName the name of the remote worker step to execute
	 * @param jobRepository the job repository to use
	 * @param messagingTemplate the messaging template to use to send step execution
	 * requests
	 */
	public RemoteStep(String name, String remoteStepName, JobRepository jobRepository,
			MessagingTemplate messagingTemplate) {
		super(jobRepository);
		setName(name);
		this.remoteStepName = remoteStepName;
		this.messagingTemplate = messagingTemplate;
		this.messageChannel = this.messagingTemplate.getDefaultDestination();
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	public void setMessageChannel(MessageChannel messageChannel) {
		this.messageChannel = messageChannel;
	}

	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		// create a step execution for the remote worker step
		JobExecution jobExecution = stepExecution.getJobExecution();
		StepExecution workerStepExecution = getJobRepository().createStepExecution(this.remoteStepName, jobExecution);

		// pass the same context to the remote worker step
		workerStepExecution.setExecutionContext(stepExecution.getExecutionContext());
		getJobRepository().update(workerStepExecution);
		getJobRepository().updateExecutionContext(workerStepExecution);

		// send step execution request and wait for the remote step to finish
		StepExecutionRequest stepExecutionRequest = new StepExecutionRequest(this.remoteStepName, jobExecution.getId(),
				workerStepExecution.getId());
		this.messagingTemplate.convertAndSend(this.messageChannel, stepExecutionRequest);
		StepExecution updatedWorkerExecution = pollRemoteStep(workerStepExecution);

		// upgrade status and context based on the remote step
		stepExecution.setExecutionContext(updatedWorkerExecution.getExecutionContext());
		stepExecution.upgradeStatus(updatedWorkerExecution.getStatus());
		stepExecution.setExitStatus(updatedWorkerExecution.getExitStatus());
	}

	private StepExecution pollRemoteStep(StepExecution workerStepExecution) throws Exception {
		Poller<StepExecution> poller = new DirectPoller<>(this.pollInterval);
		Callable<StepExecution> callable = () -> {
			StepExecution updatedExecution = getJobRepository().getStepExecution(workerStepExecution.getId());
			if (updatedExecution != null && updatedExecution.getStatus().isRunning()) {
				logger.info("Waiting for remote step to finish");
				return null;
			}
			else {
				return updatedExecution;
			}
		};
		Future<StepExecution> executionFuture = poller.poll(callable);
		if (this.timeout >= 0) {
			return executionFuture.get(this.timeout, TimeUnit.MILLISECONDS);
		}
		else {
			return executionFuture.get();
		}
	}

}
