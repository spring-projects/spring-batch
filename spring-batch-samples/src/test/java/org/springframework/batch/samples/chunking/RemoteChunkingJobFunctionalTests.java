/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.batch.samples.chunking;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The manager step of the job under test will read data and send chunks to the worker
 * (started in {@link RemoteChunkingJobFunctionalTests#setUp()}) for processing and
 * writing.
 *
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */

@SpringJUnitConfig(classes = { ManagerConfiguration.class })
@PropertySource("classpath:org/springframework/batch/samples/chunking/remote-chunking.properties")
class RemoteChunkingJobFunctionalTests {

	@Autowired
	private JobLauncher jobLauncher;

	private EmbeddedActiveMQ brokerService;

	private AnnotationConfigApplicationContext workerApplicationContext;

	@BeforeEach
	void setUp() throws Exception {
		Configuration configuration = new ConfigurationImpl().addAcceptorConfiguration("jms", "tcp://localhost:61616")
			.setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.setJMXManagementEnabled(false)
			.setJournalDatasync(false);
		this.brokerService = new EmbeddedActiveMQ().setConfiguration(configuration).start();
		this.workerApplicationContext = new AnnotationConfigApplicationContext(WorkerConfiguration.class);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.workerApplicationContext.close();
		this.brokerService.stop();
	}

	@Test
	void testRemoteChunkingJob(@Autowired Job job) throws Exception {
		// when
		JobExecution jobExecution = this.jobLauncher.run(job, new JobParameters());

		// then
		// the manager sent 2 chunks ({1, 2, 3} and {4, 5, 6}) to workers
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		assertEquals("Waited for 2 results.", jobExecution.getExitStatus().getExitDescription());
	}

}
