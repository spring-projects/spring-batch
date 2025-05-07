/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.batch.core.test.repository;

import javax.sql.DataSource;

import com.ibm.db2.jcc.DB2SimpleDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mahmoud Ben Hassine
 * @author Sukanth Gunda
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Disabled("https://github.com/spring-projects/spring-batch/issues/4828")
class Db2JobRepositoryIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName DB2_IMAGE = DockerImageName.parse("icr.io/db2_community/db2:11.5.9.0");

	@Container
	public static Db2Container db2 = new Db2Container(DB2_IMAGE).acceptLicense();

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-db2.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			DB2SimpleDataSource dataSource = new DB2SimpleDataSource();
			dataSource.setDatabaseName(db2.getDatabaseName());
			dataSource.setUser(db2.getUsername());
			dataSource.setPassword(db2.getPassword());
			dataSource.setDriverType(4);
			dataSource.setServerName(db2.getHost());
			dataSource.setPortNumber(db2.getMappedPort(Db2Container.DB2_PORT));
			dataSource.setSslConnection(false);
			return dataSource;
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
					.build())
				.build();
		}

	}

}
