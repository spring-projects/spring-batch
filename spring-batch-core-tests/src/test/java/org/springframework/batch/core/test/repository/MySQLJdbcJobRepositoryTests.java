/*
 * Copyright 2020 the original author or authors.
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

import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MySQLContainer;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class MySQLJdbcJobRepositoryTests {

	@ClassRule
	public static MySQLContainer mysql = new MySQLContainer<>();
	
	@Autowired
	private DataSource dataSource;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private JobOperator jobOperator;
	@Autowired
	private Job job;
	
	@Before
	public void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-mysql.sql"));
		databasePopulator.execute(this.dataSource);
	}

	/*
	 * This test is for issue https://github.com/spring-projects/spring-batch/issues/2202:
	 * A round trip from a `java.util.Date` JobParameter to the database and back
	 * again should preserve fractional seconds precision, otherwise a different 
	 * job instance is created while the existing one should be used.
	 * 
	 * This test ensures that round trip to the database with a `java.util.Date` 
	 * parameter ends up with a single job instance (with two job executions)
	 * being created and not two distinct job instances (with a job execution for 
	 * each one).
	 * 
	 * Note the issue does not happen if the parameter is of type Long (when using
	 * addLong("date", date.getTime()) for instance).
	 */
	@Test
	public void testDateMillisecondPrecision() throws Exception {
		// given
		Date date = new Date();
		JobParameters jobParameters = new JobParametersBuilder()
				.addDate("date", date) 
				.toJobParameters();
		
		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);
		this.jobOperator.restart(jobExecution.getId()); // should load the date parameter with fractional seconds precision here

		// then
		List<Long> jobInstances = this.jobOperator.getJobInstances("job", 0, 100);
		Assert.assertEquals(1, jobInstances.size());
		List<Long> jobExecutions = this.jobOperator.getExecutions(jobInstances.get(0));
		Assert.assertEquals(2, jobExecutions.size());
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() {
			MysqlDataSource datasource = new MysqlDataSource();
			datasource.setURL(mysql.getJdbcUrl());
			datasource.setUser(mysql.getUsername());
			datasource.setPassword(mysql.getPassword());
			return datasource;
		}

		@Bean
		public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get("job")
					.start(steps.get("step")
							.tasklet((contribution, chunkContext) -> {
								throw new Exception("expected failure");
							})
							.build())
					.build();
		}

		@Bean
		public JobOperator jobOperator(
				JobLauncher jobLauncher,
				JobRegistry jobRegistry,
				JobExplorer jobExplorer,
				JobRepository jobRepository
		) {
			SimpleJobOperator jobOperator = new SimpleJobOperator();
			jobOperator.setJobExplorer(jobExplorer);
			jobOperator.setJobLauncher(jobLauncher);
			jobOperator.setJobRegistry(jobRegistry);
			jobOperator.setJobRepository(jobRepository);
			return jobOperator;
		}

		@Bean
		public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
			JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
			jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
			return jobRegistryBeanPostProcessor;
		}
	}
}
