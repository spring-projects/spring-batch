/*
 * Copyright 2020-2023 the original author or authors.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mahmoud Ben Hassine
 * @author Sukanth Gunda
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
class MySQLJdbcJobRepositoryIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	// when implementing https://github.com/spring-projects/spring-batch/issues/3092
	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.31");

	@Container
	public static MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-mysql.sql"));
		databasePopulator.execute(this.dataSource);
	}

	/*
	 * This test is for issue https://github.com/spring-projects/spring-batch/issues/2202:
	 * A round trip from a `java.util.Date` JobParameter to the database and back again
	 * should preserve fractional seconds precision, otherwise a different job instance is
	 * created while the existing one should be used.
	 *
	 * This test ensures that round trip to the database with a `java.util.Date` parameter
	 * ends up with a single job instance (with two job executions) being created and not
	 * two distinct job instances (with a job execution for each one).
	 *
	 * Note the issue does not happen if the parameter is of type Long (when using
	 * addLong("date", date.getTime()) for instance).
	 */
	@SuppressWarnings("removal")
	@Test
	void testDateMillisecondPrecision() throws Exception {
		// given
		Date date = new Date();
		JobParameters jobParameters = new JobParametersBuilder().addDate("date", date).toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);
		this.jobOperator.restart(jobExecution.getId()); // should load the date parameter
														// with fractional seconds
														// precision here

		// then
		List<Long> jobInstances = this.jobOperator.getJobInstances("job", 0, 100);
		assertEquals(1, jobInstances.size());
		List<Long> jobExecutions = this.jobOperator.getExecutions(jobInstances.get(0));
		assertEquals(2, jobExecutions.size());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			MysqlDataSource datasource = new MysqlDataSource();
			datasource.setURL(mysql.getJdbcUrl());
			datasource.setUser(mysql.getUsername());
			datasource.setPassword(mysql.getPassword());
			datasource.setUseSSL(false);
			return datasource;
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> {
					throw new Exception("expected failure");
				}, transactionManager).build())
				.build();
		}

		@Bean
		public ConfigurableConversionService conversionService() {
			DefaultConversionService conversionService = new DefaultConversionService();
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			conversionService.addConverter(String.class, Date.class, source -> {
				try {
					return dateFormat.parse(source);
				}
				catch (ParseException e) {
					throw new RuntimeException(e);
				}
			});
			conversionService.addConverter(Date.class, String.class, dateFormat::format);
			return conversionService;
		}

	}

}
