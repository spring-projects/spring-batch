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
package org.springframework.batch.sample.remotechunking;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jms.dsl.Jms;

/**
 * This configuration class is for the worker side of the remote chunking sample.
 * It uses the {@link RemoteChunkingWorkerBuilder} to configure an
 * {@link IntegrationFlow} in order to:
 * <ul>
 *     <li>receive requests from the master</li>
 *     <li>process chunks with the configured item processor and writer</li>
 *     <li>send replies to the master</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@EnableIntegration
@PropertySource("classpath:remote-chunking.properties")
public class WorkerConfiguration {

	@Value("${broker.url}")
	private String brokerUrl;

	@Autowired
	private RemoteChunkingWorkerBuilder<Integer, Integer> remoteChunkingWorkerBuilder;

	@Bean
	public ActiveMQConnectionFactory connectionFactory() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(this.brokerUrl);
		connectionFactory.setTrustAllPackages(true);
		return connectionFactory;
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
	public DirectChannel replies() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outboundFlow(ActiveMQConnectionFactory connectionFactory) {
		return IntegrationFlows
				.from(replies())
				.handle(Jms.outboundAdapter(connectionFactory).destination("replies"))
				.get();
	}

	/*
	 * Configure worker components
	 */
	@Bean
	public ItemProcessor<Integer, Integer> itemProcessor() {
		return item -> {
			System.out.println("processing item " + item);
			return item;
		};
	}

	@Bean
	public ItemWriter<Integer> itemWriter() {
		return items -> {
			for (Integer item : items) {
				System.out.println("writing item " + item);
			}
		};
	}

	@Bean
	public IntegrationFlow workerIntegrationFlow() {
		return this.remoteChunkingWorkerBuilder
				.itemProcessor(itemProcessor())
				.itemWriter(itemWriter())
				.inputChannel(requests())
				.outputChannel(replies())
				.build();
	}

}
