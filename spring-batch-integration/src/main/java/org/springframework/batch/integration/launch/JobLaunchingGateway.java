/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.batch.integration.launch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * The {@link JobLaunchingGateway} is used to launch Batch Jobs. Internally it
 * delegates to a {@link JobLaunchingMessageHandler}.
 *
 * @author Gunnar Hillert
 *
 * @since 1.3
 */
public class JobLaunchingGateway extends AbstractReplyProducingMessageHandler {

	private final JobLaunchingMessageHandler jobLaunchingMessageHandler;

	/**
	 * Constructor taking a {@link JobLauncher} as parameter.
	 *
	 * @param jobLauncher Must not be null.
	 *
	 */
	public JobLaunchingGateway(JobLauncher jobLauncher) {
		Assert.notNull(jobLauncher, "jobLauncher must not be null.");
		this.jobLaunchingMessageHandler = new JobLaunchingMessageHandler(jobLauncher);
	}

	/**
	 * Launches a Batch Job using the provided request {@link Message}. The payload
	 * of the {@link Message} <em>must</em> be an instance of {@link JobLaunchRequest}.
	 *
	 * @param requestMessage must not be null.
	 * @return Generally a {@link JobExecution} will always be returned. An
	 * exception ({@link MessageHandlingException}) will only be thrown if there
	 * is a failure to start the job. The cause of the exception will be a
	 * {@link JobExecutionException}.
	 *
	 * @throws MessageHandlingException when a job cannot be launched
	 */
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		Assert.notNull(requestMessage, "The provided requestMessage must not be null.");

		final Object payload = requestMessage.getPayload();

		Assert.isInstanceOf(JobLaunchRequest.class, payload, "The payload must be of type JobLaunchRequest.");

		final JobLaunchRequest jobLaunchRequest = (JobLaunchRequest) payload;

		final JobExecution jobExecution;

		try {
			jobExecution = this.jobLaunchingMessageHandler.launch(jobLaunchRequest);
		} catch (JobExecutionException e) {
			throw new MessageHandlingException(requestMessage, e);
		}

		return jobExecution;

	}

}
