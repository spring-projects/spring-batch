/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.remotepartitioning.aggregating;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.sample.remotepartitioning.BasicPartitioner;
import org.springframework.batch.sample.remotepartitioning.BrokerConfiguration;
import org.springframework.batch.sample.remotepartitioning.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jms.dsl.Jms;

/**
 * This configuration class is for the master side of the remote partitioning sample.
 * The master step will create 3 partitions for workers to process.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@Import(value = {DataSourceConfiguration.class, BrokerConfiguration.class})
public class MasterConfiguration {

	private static final int GRID_SIZE = 3;

	private final JobBuilderFactory jobBuilderFactory;

	private final RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory;


	public MasterConfiguration(JobBuilderFactory jobBuilderFactory,
							   RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory) {

		this.jobBuilderFactory = jobBuilderFactory;
		this.masterStepBuilderFactory = masterStepBuilderFactory;
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
		return IntegrationFlows
				.from(requests())
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
		return IntegrationFlows
				.from(Jms.messageDrivenChannelAdapter(connectionFactory).destination("replies"))
				.channel(replies())
				.get();
	}

	/*
	 * Configure the master step
	 */
	@Bean
	public Step masterStep() {
		return this.masterStepBuilderFactory.get("masterStep")
				.partitioner("workerStep", new BasicPartitioner())
				.gridSize(GRID_SIZE)
				.outputChannel(requests())
				.inputChannel(replies())
				.build();
	}

	@Bean
	public Job remotePartitioningJob() {
		return this.jobBuilderFactory.get("remotePartitioningJob")
				.start(masterStep())
				.build();
	}

}
