/*
 * Copyright 2018-2022 the original author or authors.
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
package org.springframework.batch.sample.remotepartitioning.aggregating;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.sample.remotepartitioning.BrokerConfiguration;
import org.springframework.batch.sample.remotepartitioning.DataSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;

/**
 * This configuration class is for the worker side of the remote partitioning sample. Each
 * worker will process a partition sent by the manager step.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@Import(value = { DataSourceConfiguration.class, BrokerConfiguration.class })
public class WorkerConfiguration {

	private final RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

	public WorkerConfiguration(RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory) {
		this.workerStepBuilderFactory = workerStepBuilderFactory;
	}

	/*
	 * Configure inbound flow (requests coming from the manager)
	 */
	@Bean
	public DirectChannel requests() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow inboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(connectionFactory).destination("requests"))
				.channel(requests()).get();
	}

	/*
	 * Configure outbound flow (replies going to the manager)
	 */
	@Bean
	public DirectChannel replies() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlow.from(replies()).handle(Jms.outboundAdapter(connectionFactory).destination("replies"))
				.get();
	}

	/*
	 * Configure the worker step
	 */
	@Bean
	public Step workerStep() {
		return this.workerStepBuilderFactory.get("workerStep").inputChannel(requests()).outputChannel(replies())
				.tasklet(tasklet(null)).build();
	}

	@Bean
	@StepScope
	public Tasklet tasklet(@Value("#{stepExecutionContext['partition']}") String partition) {
		return (contribution, chunkContext) -> {
			System.out.println("processing " + partition);
			return RepeatStatus.FINISHED;
		};
	}

}
