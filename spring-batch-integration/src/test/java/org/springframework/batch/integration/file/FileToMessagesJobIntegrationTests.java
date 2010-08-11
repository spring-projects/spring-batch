/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.file;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * 
 */
@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
public class FileToMessagesJobIntegrationTests implements MessageHandler {

	@Autowired
	@Qualifier("requests")
	private SubscribableChannel requests;

	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	int count = 0;

	public void handleMessage(Message<?> message) {
		count++;
	}

	@Before
	public void setUp() {
		requests.subscribe(this);
	}

	@Test
	public void testFileSent() throws Exception {

		JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().addLong("time.stamp",
				System.currentTimeMillis()).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		// 2 chunks sent to channel (5 items and commit-interval=3)
		assertEquals(2, count);
	}

}
