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
package org.springframework.batch.samples.partitioning.remote.aggregating;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.batch.samples.partitioning.remote.BasicPartitioner;
import org.springframework.batch.samples.partitioning.remote.BrokerConfiguration;
import org.springframework.batch.samples.partitioning.remote.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;

/**
 * This configuration class is for the manager side of the remote partitioning sample. The
 * manager step will create 3 partitions for workers to process.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@EnableBatchIntegration
@Import(value = { DataSourceConfiguration.class, BrokerConfiguration.class })
public class ManagerConfiguration {

	private static final int GRID_SIZE = 3;

	private final RemotePartitioningManagerStepBuilderFactory managerStepBuilderFactory;

	public ManagerConfiguration(RemotePartitioningManagerStepBuilderFactory managerStepBuilderFactory) {

		this.managerStepBuilderFactory = managerStepBuilderFactory;
	}

	/*
	 * Configure outbound flow (requests going to workers)
	 */
	@Bean
	public DirectChannel requests() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlow.from(requests())
			.handle(Jms.outboundAdapter(connectionFactory).destination("requests"))
			.get();
	}

	/*
	 * Configure inbound flow (replies coming from workers)
	 */
	@Bean
	public DirectChannel replies() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow inboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(connectionFactory).destination("replies"))
			.channel(replies())
			.get();
	}

	/*
	 * Configure the manager step
	 */
	@Bean
	public Step managerStep() {
		return this.managerStepBuilderFactory.get("managerStep")
			.partitioner("workerStep", new BasicPartitioner())
			.gridSize(GRID_SIZE)
			.outputChannel(requests())
			.inputChannel(replies())
			.build();
	}

	@Bean
	public Job remotePartitioningJob(JobRepository jobRepository) {
		return new JobBuilder("remotePartitioningJob", jobRepository).start(managerStep()).build();
	}

}
