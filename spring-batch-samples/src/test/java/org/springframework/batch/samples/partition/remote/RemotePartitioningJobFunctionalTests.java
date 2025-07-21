/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.batch.samples.partition.remote;

import javax.sql.DataSource;

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
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for remote partitioning tests.
 *
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */

@SpringJUnitConfig
@PropertySource("classpath:org/springframework/batch/samples/partitioning/remote/remote-partitioning.properties")
public abstract class RemotePartitioningJobFunctionalTests {

	@Value("${broker.url}")
	private String brokerUrl;

	@Autowired
	protected JobOperator jobOperator;

	@Autowired
	private DataSource dataSource;

	private EmbeddedActiveMQ brokerService;

	private AnnotationConfigApplicationContext workerApplicationContext;

	protected abstract Class<?> getWorkerConfigurationClass();

	@BeforeEach
	void setUp() throws Exception {
		Configuration configuration = new ConfigurationImpl().addAcceptorConfiguration("jms", "tcp://localhost:61617")
			.setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.setJMXManagementEnabled(false)
			.setJournalDatasync(false);
		this.brokerService = new EmbeddedActiveMQ().setConfiguration(configuration).start();
		// FIXME Does not work when importing
		// org.springframework.batch.samples.common.DataSourceConfiguration?
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-drop-hsqldb.sql"));
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-hsqldb.sql"));
		databasePopulator.execute(this.dataSource);
		this.workerApplicationContext = new AnnotationConfigApplicationContext(getWorkerConfigurationClass());
	}

	@Test
	void testRemotePartitioningJob(@Autowired Job job) throws Exception {
		// when
		JobExecution jobExecution = this.jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		assertEquals(4, jobExecution.getStepExecutions().size()); // manager + 3
																	// workers
	}

	@AfterEach
	void tearDown() throws Exception {
		this.workerApplicationContext.close();
		this.brokerService.stop();
	}

}
