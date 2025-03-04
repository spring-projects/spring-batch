/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.integration.partition;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.AbstractPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.poller.DirectPoller;
import org.springframework.batch.infrastructure.poller.Poller;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A {@link PartitionHandler} that uses {@link MessageChannel} instances to send
 * instructions to remote workers and receive their responses. The {@link MessageChannel}
 * provides a nice abstraction so that the location of the workers and the transport used
 * to communicate with them can be changed at run time. The communication with the remote
 * workers does not need to be transactional or have guaranteed delivery, so a local
 * thread pool based implementation works as well as a remote web service or JMS
 * implementation. If a remote worker fails, the job will fail and can be restarted to
 * pick up missing messages and processing. The remote workers need access to the Spring
 * Batch {@link JobRepository} so that the shared state across those restarts can be
 * managed centrally.
 * <p>
 * While a {@link org.springframework.messaging.MessageChannel} is used for sending the
 * requests to the workers, the worker's responses can be obtained in one of two ways:
 * <ul>
 * <li>A reply channel - Workers will respond with messages that will be aggregated via
 * this component.</li>
 * <li>Polling the job repository - Since the state of each worker is maintained
 * independently within the job repository, we can poll the store to determine the state
 * without the need of the workers to formally respond.</li>
 * </ul>
 *
 * Note: The reply channel for this is instance based. Sharing this component across
 * multiple step instances may result in the crossing of messages. It's recommended that
 * this component be step or job scoped.
 *
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
@MessageEndpoint
public class MessageChannelPartitionHandler extends AbstractPartitionHandler implements InitializingBean {

	private static final Log logger = LogFactory.getLog(MessageChannelPartitionHandler.class);

	private MessagingTemplate messagingGateway;

	private String stepName;

	private long pollInterval = 10000;

	private JobRepository jobRepository;

	private boolean pollRepositoryForResults;

	private long timeout = -1;

	/**
	 * pollable channel for the replies
	 */
	private PollableChannel replyChannel;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(stepName != null, "A step name must be provided for the remote workers.");
		Assert.state(messagingGateway != null, "The MessagingOperations must be set");

		pollRepositoryForResults = jobRepository != null;

		if (pollRepositoryForResults) {
			logger.debug("MessageChannelPartitionHandler is configured to poll the job repository for worker results");
		}
		else {
			logger.debug("MessageChannelPartitionHandler is configured to use a reply channel for worker results");
			if (replyChannel == null) {
				logger.info("No reply channel configured, using a QueueChannel as the default reply channel.");
				replyChannel = new QueueChannel();
			}
		}

	}

	/**
	 * When using job repository polling, the time limit to wait.
	 * @param timeout milliseconds to wait, defaults to -1 (no timeout).
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * {@link JobRepository} to use to query the job repository. This is required when
	 * using job repository polling.
	 * @param jobRepository {@link JobRepository} to use for lookups
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * How often to poll the job repository for the status of the workers.
	 * @param pollInterval milliseconds between polls, defaults to 10000 (10 seconds).
	 */
	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	/**
	 * A pre-configured gateway for sending and receiving messages to the remote workers.
	 * Using this property allows a large degree of control over the timeouts and other
	 * properties of the send. It should have channels set up internally:
	 * <ul>
	 * <li>request channel capable of accepting {@link StepExecutionRequest} payloads</li>
	 * <li>reply channel that returns a list of {@link StepExecution} results</li>
	 * </ul>
	 * The timeout for the reply should be set sufficiently long that the remote steps
	 * have time to complete.
	 * @param messagingGateway the
	 * {@link org.springframework.integration.core.MessagingTemplate} to set
	 */
	public void setMessagingOperations(MessagingTemplate messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	/**
	 * The name of the {@link Step} that will be used to execute the partitioned
	 * {@link StepExecution}. This is a regular Spring Batch step, with all the business
	 * logic required to complete an execution based on the input parameters in its
	 * {@link StepExecution} context. The name will be translated into a {@link Step}
	 * instance by the remote worker.
	 * @param stepName the name of the {@link Step} instance to execute business logic
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @param messages the messages to be aggregated
	 * @return the list as it was passed in
	 */
	@Aggregator(sendPartialResultsOnExpiry = "true")
	public List<?> aggregate(@Payloads List<?> messages) {
		return messages;
	}

	public void setReplyChannel(PollableChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Sends {@link StepExecutionRequest} objects to the request channel of the
	 * {@link MessagingTemplate}, and then receives the result back as a list of
	 * {@link StepExecution} on a reply channel. Use the {@link #aggregate(List)} method
	 * as an aggregator of the individual remote replies. The receive timeout needs to be
	 * set realistically in the {@link MessagingTemplate} <b>and</b> the aggregator, so
	 * that there is a good chance of all work being done.
	 *
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	@Override
	protected Set<StepExecution> doHandle(StepExecution managerStepExecution,
			Set<StepExecution> partitionStepExecutions) throws Exception {

		if (CollectionUtils.isEmpty(partitionStepExecutions)) {
			return partitionStepExecutions;
		}

		int count = 0;

		long jobExecutionId = managerStepExecution.getJobExecution().getId();
		for (StepExecution stepExecution : partitionStepExecutions) {
			Message<StepExecutionRequest> request = createMessage(count++, partitionStepExecutions.size(),
					new StepExecutionRequest(stepName, stepExecution.getId()), jobExecutionId, replyChannel);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending request: " + request);
			}
			messagingGateway.send(request);
		}

		if (!pollRepositoryForResults) {
			return receiveReplies(replyChannel);
		}
		else {
			return pollReplies(managerStepExecution, partitionStepExecutions);
		}
	}

	private Set<StepExecution> pollReplies(StepExecution managerStepExecution, final Set<StepExecution> split)
			throws Exception {
		Set<Long> partitionStepExecutionIds = split.stream().map(StepExecution::getId).collect(Collectors.toSet());

		Callable<Set<StepExecution>> callback = () -> {
			JobExecution jobExecution = jobRepository.getJobExecution(managerStepExecution.getJobExecutionId());
			Set<StepExecution> finishedStepExecutions = jobExecution.getStepExecutions()
				.stream()
				.filter(stepExecution -> partitionStepExecutionIds.contains(stepExecution.getId()))
				.filter(stepExecution -> !stepExecution.getStatus().isRunning())
				.collect(Collectors.toSet());

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Currently waiting on %s partitions to finish", split.size()));
			}

			if (finishedStepExecutions.size() == split.size()) {
				return finishedStepExecutions;
			}
			else {
				return null;
			}
		};

		Poller<Set<StepExecution>> poller = new DirectPoller<>(pollInterval);
		Future<Set<StepExecution>> resultsFuture = poller.poll(callback);

		if (timeout >= 0) {
			return resultsFuture.get(timeout, TimeUnit.MILLISECONDS);
		}
		else {
			return resultsFuture.get();
		}
	}

	@SuppressWarnings("unchecked")
	private Set<StepExecution> receiveReplies(PollableChannel currentReplyChannel) {
		Message<Collection<StepExecution>> message = (Message<Collection<StepExecution>>) messagingGateway
			.receive(currentReplyChannel);

		if (message == null) {
			throw new MessageTimeoutException("Timeout occurred before all partitions returned");
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Received replies: " + message);
		}

		Collection<StepExecution> payload = message.getPayload();
		return payload instanceof Set ? (Set<StepExecution>) payload : new HashSet<>(message.getPayload());
	}

	private Message<StepExecutionRequest> createMessage(int sequenceNumber, int sequenceSize,
			StepExecutionRequest stepExecutionRequest, long jobExecutionId, PollableChannel replyChannel) {
		return MessageBuilder.withPayload(stepExecutionRequest)
			.setSequenceNumber(sequenceNumber)
			.setSequenceSize(sequenceSize)
			.setCorrelationId(jobExecutionId + ":" + stepExecutionRequest.getStepName())
			.setReplyChannel(replyChannel)
			.build();
	}

}
