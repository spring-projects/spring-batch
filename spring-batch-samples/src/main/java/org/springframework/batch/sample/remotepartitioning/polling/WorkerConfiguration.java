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
package org.springframework.batch.sample.remotepartitioning.polling;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.batch.integration.partition.StepExecutionRequestHandler;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.sample.remotepartitioning.BrokerConfiguration;
import org.springframework.batch.sample.remotepartitioning.DataSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jms.dsl.Jms;

/**
 * This configuration class is for the worker side of the remote partitioning sample.
 * Each worker will process a partition sent by the master step.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableIntegration
@Import(value = {DataSourceConfiguration.class, BrokerConfiguration.class})
public class WorkerConfiguration {

	private final StepBuilderFactory stepBuilderFactory;

	private final ApplicationContext applicationContext;

	private final JobExplorer jobExplorer;


	public WorkerConfiguration(StepBuilderFactory stepBuilderFactory,
								JobExplorer jobExplorer,
								ApplicationContext applicationContext) {

		this.stepBuilderFactory = stepBuilderFactory;
		this.applicationContext = applicationContext;
		this.jobExplorer = jobExplorer;
	}

	/*
	 * Configure inbound flow (requests coming from the master)
	 */
	@Bean
	public DirectChannel requests() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow inboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlows
				.from(Jms.messageDrivenChannelAdapter(connectionFactory).destination("requests"))
				.channel(requests())
				.get();
	}

	/*
	 * Configure outbound flow (replies going to the master)
	 */
	@Bean
	public NullChannel replies() {
		return new NullChannel(); // replies are discarded (since the master is polling the job repository)
	}

	/*
	 * Configure worker components
	 */
	@Bean
	@ServiceActivator(inputChannel = "requests", outputChannel = "replies")
	public StepExecutionRequestHandler stepExecutionRequestHandler() {
		StepExecutionRequestHandler stepExecutionRequestHandler = new StepExecutionRequestHandler();
		stepExecutionRequestHandler.setJobExplorer(this.jobExplorer);
		BeanFactoryStepLocator stepLocator = new BeanFactoryStepLocator();
		stepLocator.setBeanFactory(this.applicationContext);
		stepExecutionRequestHandler.setStepLocator(stepLocator);
		return stepExecutionRequestHandler;
	}

	@Bean
	public Step slaveStep() {
		return this.stepBuilderFactory.get("slaveStep")
				.tasklet(getTasklet(null))
				.build();
	}

	@Bean
	@StepScope
	public Tasklet getTasklet(@Value("#{stepExecutionContext['partition']}") String partition) {
		return (contribution, chunkContext) -> {
			System.out.println("processing " + partition);
			return RepeatStatus.FINISHED;
		};
	}

}
