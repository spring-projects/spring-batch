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
package org.springframework.batch.samples.remotestep;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.remote.RemoteStep;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@EnableIntegration
@Import(value = { InfrastructureConfiguration.class })
public class ManagerConfiguration {

	/*
	 * Configure outbound flow (requests going to workers)
	 */
	@Bean
	public DirectChannel requests() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outboundFlow(ActiveMQConnectionFactory connectionFactory, BeanFactory beanFactory) {
		StandardIntegrationFlow integrationFlow = IntegrationFlow.from(requests())
			.handle(Jms.outboundAdapter(connectionFactory).destination("requests"))
			.get();
		return integrationFlow;
	}

	@Bean
	public MessagingTemplate messagingTemplate(MessageChannel requests) {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(requests);
		messagingTemplate.setReceiveTimeout(60000);
		messagingTemplate.setSendTimeout(10000);
		return messagingTemplate;
	}

	@Bean
	public Step step(MessagingTemplate messagingTemplate, JobRepository jobRepository) {
		return new RemoteStep("step", "workerStep", jobRepository, messagingTemplate);
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ManagerConfiguration.class);
		org.apache.activemq.artemis.core.config.Configuration configuration = new ConfigurationImpl()
			.addAcceptorConfiguration("jms", "tcp://localhost:61617")
			.setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.setJMXManagementEnabled(false)
			.setJournalDatasync(false);

		EmbeddedActiveMQ brokerService = new EmbeddedActiveMQ().setConfiguration(configuration).start();
		StandardIntegrationFlow integrationFlow = context.getBean(StandardIntegrationFlow.class);
		integrationFlow.start();

		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());
		System.out.println("jobExecution = " + jobExecution);
		integrationFlow.stop();
		brokerService.stop();
	}

}
