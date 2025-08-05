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

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
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
class SQLServerJobRepositoryIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName SQLSERVER_IMAGE = DockerImageName
		.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04");

	@Container
	public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(SQLSERVER_IMAGE).acceptLicense();

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-sqlserver.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobOperator.start(this.job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			SQLServerDataSource dataSource = new SQLServerDataSource();
			dataSource.setUser(sqlserver.getUsername());
			dataSource.setPassword(sqlserver.getPassword());
			dataSource.setURL(sqlserver.getJdbcUrl());
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
