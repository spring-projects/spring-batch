/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.samples.amqp;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * <p>
 * Ensure a RabbitMQ instance is running, modifying default.amqp.properties if needed.
 * Execute the {@link AmqpMessageProducer#main(String[])} method in order for messages
 * will be written to the "test.inbound" queue.
 * </p>
 *
 * <p>
 * Run this test and the job will read those messages, process them and write them to the
 * "test.outbound" queue for inspection.
 * </p>
 */

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/amqp/job/amqp-example-job.xml",
		"/simple-job-launcher-context.xml" })
@Testcontainers(disabledWithoutDocker = true)
class AmqpJobFunctionalTests {

	private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:4.1.2");

	@Container
	public static RabbitMQContainer rabbitmq = new RabbitMQContainer(RABBITMQ_IMAGE);

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Autowired
	private JobRepository jobRepository;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		this.jobOperatorTestUtils.startJob();

		// when
		int count = jobRepository.findJobInstances("amqp-example-job").size();

		// then
		assertTrue(count > 0);
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(AmqpJobConfiguration.class,
				AmqpConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		jobOperator.start(job, new JobParameters());

		// then
		JobRepository localJobRepository = context.getBean(JobRepository.class);
		int count = localJobRepository.findJobInstances("amqp-config-job").size();
		assertTrue(count > 0);
	}

	@Configuration
	static class AmqpConfiguration {

		public final static String QUEUE_NAME = "rabbitmq.test.queue";

		public final static String EXCHANGE_NAME = "rabbitmq.test.exchange";

		/**
		 * @return {@link CachingConnectionFactory} to be used by the {@link AmqpTemplate}
		 */
		@Bean
		public CachingConnectionFactory connectionFactory() {
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmq.getHost(),
					rabbitmq.getAmqpPort());
			AmqpAdmin admin = new RabbitAdmin(connectionFactory);
			admin.declareQueue(new Queue(AmqpConfiguration.QUEUE_NAME));
			admin.declareExchange(new TopicExchange(AmqpConfiguration.EXCHANGE_NAME));
			admin.declareBinding(new Binding(AmqpConfiguration.QUEUE_NAME, Binding.DestinationType.QUEUE,
					AmqpConfiguration.EXCHANGE_NAME, "#", null));
			return connectionFactory;
		}

		/**
		 * @return {@link AmqpTemplate} to be used for the {@link ItemWriter}
		 */
		@Bean
		public AmqpTemplate rabbitOutputTemplate(CachingConnectionFactory connectionFactory) {
			RabbitTemplate template = new RabbitTemplate(connectionFactory);
			template.setMessageConverter(new Jackson2JsonMessageConverter());
			template.setExchange(EXCHANGE_NAME);
			return template;
		}

		/**
		 * @return {@link AmqpTemplate} to be used for the {@link ItemReader}.
		 */
		@Bean
		public RabbitTemplate rabbitInputTemplate(CachingConnectionFactory connectionFactory) {
			RabbitTemplate template = new RabbitTemplate(connectionFactory);
			template.setMessageConverter(new Jackson2JsonMessageConverter());
			template.setDefaultReceiveQueue(QUEUE_NAME);
			return template;
		}

	}

}
