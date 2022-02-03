/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.batch.sample;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.sample.amqp.AmqpConfiguration;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * <p>
 * Ensure a RabbitMQ instance is running, modifying default.amqp.properties if needed.
 * Execute the
 * {@link org.springframework.batch.sample.rabbitmq.amqp.AmqpMessageProducer#main(String[])}
 * method in order for messages will be written to the "test.inbound" queue.
 * </p>
 *
 * <p>
 * Run this test and the job will read those messages, process them and write them to the
 * "test.outbound" queue for inspection.
 * </p>
 */

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/amqp-example-job.xml", "/job-runner-context.xml" })
class AMQPJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobExplorer jobExplorer;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		this.jobLauncherTestUtils.launchJob();

		// when
		int count = jobExplorer.getJobInstances("amqp-example-job", 0, 1).size();

		// then
		assertTrue(count > 0);
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(AmqpConfiguration.class);
		initializeExchange(context.getBean(CachingConnectionFactory.class));
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		jobLauncher.run(job, new JobParameters());

		// then
		JobExplorer localJobExplorer = context.getBean(JobExplorer.class);
		int count = localJobExplorer.getJobInstances("amqp-config-job", 0, 1).size();
		assertTrue(count > 0);
	}

	private void initializeExchange(CachingConnectionFactory connectionFactory) {
		AmqpAdmin admin = new RabbitAdmin(connectionFactory);
		admin.declareQueue(new Queue(AmqpConfiguration.QUEUE_NAME));
		admin.declareExchange(new TopicExchange(AmqpConfiguration.EXCHANGE_NAME));
		admin.declareBinding(new Binding(AmqpConfiguration.QUEUE_NAME, Binding.DestinationType.QUEUE,
				AmqpConfiguration.EXCHANGE_NAME, "#", null));
	}

}
