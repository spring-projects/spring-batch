/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void testLaunchJob() throws Exception {
		// given
		this.jobLauncherTestUtils.launchJob();

		// when
		int count = jobExplorer.getJobInstances("amqp-example-job", 0, 1).size();

		// then
		assertTrue(count > 0);

	}

}
