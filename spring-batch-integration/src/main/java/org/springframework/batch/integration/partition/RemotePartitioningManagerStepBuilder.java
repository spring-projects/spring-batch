/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.integration.partition;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.PartitionStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;

/**
 * Builder for a manager step in a remote partitioning setup. This builder creates and
 * sets a {@link MessageChannelPartitionHandler} on the manager step.
 *
 * <p>
 * If no {@code messagingTemplate} is provided through
 * {@link RemotePartitioningManagerStepBuilder#messagingTemplate(MessagingTemplate)}, this
 * builder will create one and set its default channel to the {@code outputChannel}
 * provided through
 * {@link RemotePartitioningManagerStepBuilder#outputChannel(MessageChannel)}.
 * </p>
 *
 * <p>
 * If a {@code messagingTemplate} is provided, it is assumed that it is fully configured
 * and that its default channel is set to an output channel on which requests to workers
 * will be sent.
 * </p>
 *
 * @since 4.2
 * @author Mahmoud Ben Hassine
 */
public class RemotePartitioningManagerStepBuilder extends PartitionStepBuilder {

	private static final long DEFAULT_POLL_INTERVAL = 10000L;

	private static final long DEFAULT_TIMEOUT = -1L;

	private MessagingTemplate messagingTemplate;

	private MessageChannel inputChannel;

	private MessageChannel outputChannel;

	private JobExplorer jobExplorer;

	private BeanFactory beanFactory;

	private long pollInterval = DEFAULT_POLL_INTERVAL;

	private long timeout = DEFAULT_TIMEOUT;

	/**
	 * Create a new {@link RemotePartitioningManagerStepBuilder}.
	 * @param stepName name of the manager step
	 * @param jobRepository job repository to which the step should report to
	 * @since 5.0
	 */
	public RemotePartitioningManagerStepBuilder(String stepName, JobRepository jobRepository) {
		super(new StepBuilder(stepName, jobRepository));
	}

	/**
	 * Set the input channel on which replies from workers will be received.
	 * @param inputChannel the input channel
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningManagerStepBuilder inputChannel(MessageChannel inputChannel) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.inputChannel = inputChannel;
		return this;
	}

	/**
	 * Set the output channel on which requests to workers will be sent. By using this
	 * setter, a default messaging template will be created and the output channel will be
	 * set as its default channel.
	 * <p>
	 * Use either this setter or
	 * {@link RemotePartitioningManagerStepBuilder#messagingTemplate(MessagingTemplate)}
	 * to provide a fully configured messaging template.
	 * </p>
	 * @param outputChannel the output channel.
	 * @return this builder instance for fluent chaining
	 * @see RemotePartitioningManagerStepBuilder#messagingTemplate(MessagingTemplate)
	 */
	public RemotePartitioningManagerStepBuilder outputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		return this;
	}

	/**
	 * Set the {@link MessagingTemplate} to use to send data to workers. <strong>The
	 * default channel of the messaging template must be set</strong>.
	 * <p>
	 * Use either this setter to provide a fully configured messaging template or provide
	 * an output channel through
	 * {@link RemotePartitioningManagerStepBuilder#outputChannel(MessageChannel)} and a
	 * default messaging template will be created.
	 * </p>
	 * @param messagingTemplate the messaging template to use
	 * @return this builder instance for fluent chaining
	 * @see RemotePartitioningManagerStepBuilder#outputChannel(MessageChannel)
	 */
	public RemotePartitioningManagerStepBuilder messagingTemplate(MessagingTemplate messagingTemplate) {
		Assert.notNull(messagingTemplate, "messagingTemplate must not be null");
		this.messagingTemplate = messagingTemplate;
		return this;
	}

	/**
	 * Set the job explorer.
	 * @param jobExplorer the job explorer to use.
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningManagerStepBuilder jobExplorer(JobExplorer jobExplorer) {
		Assert.notNull(jobExplorer, "jobExplorer must not be null");
		this.jobExplorer = jobExplorer;
		return this;
	}

	/**
	 * How often to poll the job repository for the status of the workers. Defaults to 10
	 * seconds.
	 * @param pollInterval the poll interval value in milliseconds
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningManagerStepBuilder pollInterval(long pollInterval) {
		Assert.isTrue(pollInterval > 0, "The poll interval must be greater than zero");
		this.pollInterval = pollInterval;
		return this;
	}

	/**
	 * When using job repository polling, the time limit to wait. Defaults to -1 (no
	 * timeout).
	 * @param timeout the timeout value in milliseconds
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningManagerStepBuilder timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Set the bean factory.
	 * @param beanFactory the bean factory to use
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningManagerStepBuilder beanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		return this;
	}

	@Override
	public Step build() {
		Assert.state(this.outputChannel == null || this.messagingTemplate == null,
				"You must specify either an outputChannel or a messagingTemplate but not both.");

		// configure messaging template
		if (this.messagingTemplate == null) {
			this.messagingTemplate = new MessagingTemplate();
			this.messagingTemplate.setDefaultChannel(this.outputChannel);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No messagingTemplate was provided, using a default one");
			}
		}

		// Configure the partition handler
		final MessageChannelPartitionHandler partitionHandler = new MessageChannelPartitionHandler();
		partitionHandler.setStepName(getStepName());
		partitionHandler.setGridSize(getGridSize());
		partitionHandler.setMessagingOperations(this.messagingTemplate);

		if (isPolling()) {
			partitionHandler.setJobExplorer(this.jobExplorer);
			partitionHandler.setPollInterval(this.pollInterval);
			partitionHandler.setTimeout(this.timeout);
		}
		else {
			PollableChannel replies = new QueueChannel();
			partitionHandler.setReplyChannel(replies);
			StandardIntegrationFlow standardIntegrationFlow = IntegrationFlow.from(this.inputChannel)
				.aggregate(aggregatorSpec -> aggregatorSpec.processor(partitionHandler))
				.channel(replies)
				.get();
			IntegrationFlowContext integrationFlowContext = this.beanFactory.getBean(IntegrationFlowContext.class);
			integrationFlowContext.registration(standardIntegrationFlow).autoStartup(false).register();
		}

		try {
			partitionHandler.afterPropertiesSet();
			super.partitionHandler(partitionHandler);
		}
		catch (Exception e) {
			throw new BeanCreationException("Unable to create a manager step for remote partitioning", e);
		}

		return super.build();
	}

	private boolean isPolling() {
		return this.inputChannel == null;
	}

	@Override
	public RemotePartitioningManagerStepBuilder partitioner(String workerStepName, Partitioner partitioner) {
		super.partitioner(workerStepName, partitioner);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder gridSize(int gridSize) {
		super.gridSize(gridSize);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder step(Step step) {
		super.step(step);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder splitter(StepExecutionSplitter splitter) {
		super.splitter(splitter);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder aggregator(StepExecutionAggregator aggregator) {
		super.aggregator(aggregator);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder startLimit(int startLimit) {
		super.startLimit(startLimit);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder listener(Object listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder listener(StepExecutionListener listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemotePartitioningManagerStepBuilder allowStartIfComplete(boolean allowStartIfComplete) {
		super.allowStartIfComplete(allowStartIfComplete);
		return this;
	}

	/**
	 * This method will throw a {@link UnsupportedOperationException} since the partition
	 * handler of the manager step will be automatically set to an instance of
	 * {@link MessageChannelPartitionHandler}.
	 * <p>
	 * When building a manager step for remote partitioning using this builder, no
	 * partition handler must be provided.
	 * @param partitionHandler a partition handler
	 * @return this builder instance for fluent chaining
	 * @throws UnsupportedOperationException if a partition handler is provided
	 */
	@Override
	public RemotePartitioningManagerStepBuilder partitionHandler(PartitionHandler partitionHandler)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("When configuring a manager step "
				+ "for remote partitioning using the RemotePartitioningManagerStepBuilder, "
				+ "the partition handler will be automatically set to an instance "
				+ "of MessageChannelPartitionHandler. The partition handler must " + "not be provided in this case.");
	}

}
