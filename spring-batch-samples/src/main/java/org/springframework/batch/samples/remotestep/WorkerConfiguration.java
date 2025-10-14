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

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.batch.integration.partition.StepExecutionRequestHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@EnableIntegration
@Import(value = { InfrastructureConfiguration.class })
public class WorkerConfiguration {

	@Bean
	public Step workerStep(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return new StepBuilder("workerStep", jobRepository).tasklet((transaction, chunkContext) -> {
			System.out.println("Worker step started...");
			Thread.sleep(20000); // Simulate a long-running task
			System.out.println("Worker step executed");
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	/*
	 * Configure inbound flow (requests coming from the manager)
	 */
	@Bean
	public DirectChannel requests() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow inboundFlow(ActiveMQConnectionFactory connectionFactory, JobRepository jobRepository,
			StepLocator stepLocator) {
		StepExecutionRequestHandler stepExecutionRequestHandler = new StepExecutionRequestHandler();
		stepExecutionRequestHandler.setJobRepository(jobRepository);
		stepExecutionRequestHandler.setStepLocator(stepLocator);
		return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(connectionFactory).destination("requests"))
			.channel(requests())
			.handle(stepExecutionRequestHandler, "handle")
			.channel(new NullChannel()) // No replies are sent back to the manager
			.get();
	}

	@Bean
	public StepLocator stepLocator(BeanFactory beanFactory) {
		BeanFactoryStepLocator beanFactoryStepLocator = new BeanFactoryStepLocator();
		beanFactoryStepLocator.setBeanFactory(beanFactory);
		return beanFactoryStepLocator;
	}

	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WorkerConfiguration.class);
		StandardIntegrationFlow integrationFlow = context.getBean(StandardIntegrationFlow.class);
		integrationFlow.start();
		System.out.println("Worker started, waiting for requests...");
		System.out.println("Press Enter to terminate");
		System.in.read();
		integrationFlow.stop();
		context.close();
	}

}