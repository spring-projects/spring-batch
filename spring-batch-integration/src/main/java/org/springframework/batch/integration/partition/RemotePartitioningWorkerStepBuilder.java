/*
 * Copyright 2018 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.builder.FlowStepBuilder;
import org.springframework.batch.core.step.builder.JobStepBuilder;
import org.springframework.batch.core.step.builder.PartitionStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Builder for a worker step in a remote partitioning setup. This builder
 * creates an {@link IntegrationFlow} that:
 *
 * <ul>
 *     <li>listens to {@link StepExecutionRequest}s coming from the master
 *     on the input channel</li>
 *     <li>invokes the {@link StepExecutionRequestHandler} to execute the worker
 *     step for each incoming request. The worker step is located using the provided
 *     {@link StepLocator}. If no {@link StepLocator} is provided, a {@link BeanFactoryStepLocator}
 *     configured with the current {@link BeanFactory} will be used
 *     <li>replies to the master on the output channel (when the master step is
 *     configured to aggregate replies from workers). If no output channel
 *     is provided, a {@link NullChannel} will be used (assuming the master side
 *     is configured to poll the job repository for workers status)</li>
 * </ul>
 *
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
public class RemotePartitioningWorkerStepBuilder extends StepBuilder {

	private static final String SERVICE_ACTIVATOR_METHOD_NAME = "handle";
	private static final Log logger = LogFactory.getLog(RemotePartitioningWorkerStepBuilder.class);

	private MessageChannel inputChannel;
	private MessageChannel outputChannel;
	private JobExplorer jobExplorer;
	private StepLocator stepLocator;
	private BeanFactory beanFactory;

	/**
	 * Initialize a step builder for a step with the given name.
	 * @param name the name of the step
	 */
	public RemotePartitioningWorkerStepBuilder(String name) {
		super(name);
	}

	/**
	 * Set the input channel on which step execution requests sent by the master
	 * are received.
	 * @param inputChannel the input channel
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningWorkerStepBuilder inputChannel(MessageChannel inputChannel) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.inputChannel = inputChannel;
		return this;
	}

	/**
	 * Set the output channel on which replies will be sent to the master step.
	 * @param outputChannel the input channel
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningWorkerStepBuilder outputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		return this;
	}

	/**
	 * Set the job explorer.
	 * @param jobExplorer the job explorer to use
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningWorkerStepBuilder jobExplorer(JobExplorer jobExplorer) {
		Assert.notNull(jobExplorer, "jobExplorer must not be null");
		this.jobExplorer = jobExplorer;
		return this;
	}

	/**
	 * Set the step locator used to locate the worker step to execute.
	 * @param stepLocator the step locator to use
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningWorkerStepBuilder stepLocator(StepLocator stepLocator) {
		Assert.notNull(stepLocator, "stepLocator must not be null");
		this.stepLocator = stepLocator;
		return this;
	}

	/**
	 * Set the bean factory.
	 * @param beanFactory the bean factory
	 * @return this builder instance for fluent chaining
	 */
	public RemotePartitioningWorkerStepBuilder beanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder repository(JobRepository jobRepository) {
		super.repository(jobRepository);
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder transactionManager(PlatformTransactionManager transactionManager) {
		super.transactionManager(transactionManager);
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder startLimit(int startLimit) {
		super.startLimit(startLimit);
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder listener(Object listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder listener(StepExecutionListener listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemotePartitioningWorkerStepBuilder allowStartIfComplete(boolean allowStartIfComplete) {
		super.allowStartIfComplete(allowStartIfComplete);
		return this;
	}

	@Override
	public TaskletStepBuilder tasklet(Tasklet tasklet) {
		configureWorkerIntegrationFlow();
		return super.tasklet(tasklet);
	}

	@Override
	public <I, O> SimpleStepBuilder<I, O> chunk(int chunkSize) {
		configureWorkerIntegrationFlow();
		return super.chunk(chunkSize);
	}

	@Override
	public <I, O> SimpleStepBuilder<I, O> chunk(CompletionPolicy completionPolicy) {
		configureWorkerIntegrationFlow();
		return super.chunk(completionPolicy);
	}

	@Override
	public PartitionStepBuilder partitioner(String stepName, Partitioner partitioner) {
		configureWorkerIntegrationFlow();
		return super.partitioner(stepName, partitioner);
	}

	@Override
	public PartitionStepBuilder partitioner(Step step) {
		configureWorkerIntegrationFlow();
		return super.partitioner(step);
	}

	@Override
	public JobStepBuilder job(Job job) {
		configureWorkerIntegrationFlow();
		return super.job(job);
	}

	@Override
	public FlowStepBuilder flow(Flow flow) {
		configureWorkerIntegrationFlow();
		return super.flow(flow);
	}

	/**
	 * Create an {@link IntegrationFlow} with a {@link StepExecutionRequestHandler}
	 * configured as a service activator listening to the input channel and replying
	 * on the output channel.
	 */
	private void configureWorkerIntegrationFlow() {
		Assert.notNull(this.inputChannel, "An InputChannel must be provided");
		Assert.notNull(this.jobExplorer, "A JobExplorer must be provided");

		if (this.stepLocator == null) {
			BeanFactoryStepLocator beanFactoryStepLocator = new BeanFactoryStepLocator();
			beanFactoryStepLocator.setBeanFactory(this.beanFactory);
			this.stepLocator = beanFactoryStepLocator;
		}
		if (this.outputChannel == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("The output channel is set to a NullChannel. " +
						"The master step must poll the job repository for workers status.");
			}
			this.outputChannel = new NullChannel();
		}

		StepExecutionRequestHandler stepExecutionRequestHandler = new StepExecutionRequestHandler();
		stepExecutionRequestHandler.setJobExplorer(this.jobExplorer);
		stepExecutionRequestHandler.setStepLocator(this.stepLocator);

		StandardIntegrationFlow standardIntegrationFlow = IntegrationFlows
				.from(this.inputChannel)
				.handle(stepExecutionRequestHandler, SERVICE_ACTIVATOR_METHOD_NAME)
				.channel(this.outputChannel)
				.get();
		IntegrationFlowContext integrationFlowContext = this.beanFactory.getBean(IntegrationFlowContext.class);
		integrationFlowContext.registration(standardIntegrationFlow)
				.autoStartup(false)
				.register();
	}

}
