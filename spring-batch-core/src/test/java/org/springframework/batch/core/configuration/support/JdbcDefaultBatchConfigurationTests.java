/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Yanming Zhou
 */
class JdbcDefaultBatchConfigurationTests {

	private static final String jobInstanceIncrementerName = "JOB_SEQ";

	private static final String jobExecutionIncrementerName = "JE_SEQ";

	private static final String stepExecutionIncrementerName = "SE_SEQ";

	@Test
	void testCustomIncrementerName() throws Exception {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions
			.assertEquals(1L,
					jdbcTemplate.queryForObject("select count(*) from "
							+ AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + jobInstanceIncrementerName,
							Long.class));
		Assertions.assertEquals(0L,
				jdbcTemplate.queryForObject("select count(*) from " + AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX
						+ AbstractJdbcBatchMetadataDao.DEFAULT_JOB_INSTANCE_INCREMENTER_NAME, Long.class));
		Assertions
			.assertEquals(1L,
					jdbcTemplate.queryForObject("select count(*) from "
							+ AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + jobExecutionIncrementerName,
							Long.class));
		Assertions.assertEquals(0L,
				jdbcTemplate.queryForObject("select count(*) from " + AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX
						+ AbstractJdbcBatchMetadataDao.DEFAULT_JOB_EXECUTION_INCREMENTER_NAME, Long.class));
		Assertions
			.assertEquals(1L,
					jdbcTemplate.queryForObject("select count(*) from "
							+ AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + stepExecutionIncrementerName,
							Long.class));
		Assertions
			.assertEquals(0L,
					jdbcTemplate.queryForObject(
							"select count(*) from " + AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX
									+ AbstractJdbcBatchMetadataDao.DEFAULT_STEP_EXECUTION_INCREMENTER_NAME,
							Long.class));
	}

	@Configuration
	static class MyJobConfiguration extends JdbcDefaultBatchConfiguration {

		@Bean
		public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			Tasklet myTasklet = (contribution, chunkContext) -> RepeatStatus.FINISHED;
			return new StepBuilder("myStep", jobRepository).tasklet(myTasklet, transactionManager).build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step myStep) {
			return new JobBuilder("job", jobRepository).start(myStep).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.addScript(
						"/org/springframework/batch/core/configuration/support/custom-incrementer-name-schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Override
		protected String getJobInstanceIncrementerName() {
			return jobInstanceIncrementerName;
		}

		@Override
		protected String getJobExecutionIncrementerName() {
			return jobExecutionIncrementerName;
		}

		@Override
		protected String getStepExecutionIncrementerName() {
			return stepExecutionIncrementerName;
		}

	}

	@Configuration
	static class MyJobConfigurationWithCustomInfrastructureBean extends MyJobConfiguration {

		@Bean
		@Override
		public JobRepository jobRepository() {
			return new ResourcelessJobRepository();
		}

	}

}