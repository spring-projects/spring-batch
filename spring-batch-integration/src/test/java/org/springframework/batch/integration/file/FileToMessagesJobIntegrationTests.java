/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.integration.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 *
 */
@SpringJUnitConfig
class FileToMessagesJobIntegrationTests implements MessageHandler {

	@Autowired
	@Qualifier("requests")
	private SubscribableChannel requests;

	@Autowired
	private Job job;

	@Autowired
	private JobOperator jobOperator;

	int count = 0;

	@Override
	public void handleMessage(Message<?> message) {
		count++;
	}

	@BeforeEach
	void setUp() {
		requests.subscribe(this);
	}

	@Test
	void testFileSent() throws Exception {

		JobExecution execution = jobOperator.start(job,
				new JobParametersBuilder().addLong("time.stamp", System.currentTimeMillis()).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		// 2 chunks sent to channel (5 items and commit-interval=3)
		assertEquals(2, count);
	}

}
