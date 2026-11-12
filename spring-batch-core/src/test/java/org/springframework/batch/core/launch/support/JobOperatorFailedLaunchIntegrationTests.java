/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tests the state of the job repository when launching a job fails because a job
 * parameter cannot be converted to a {@link String} to be persisted, or because the job
 * parameters are rejected by the job's validator.
 *
 * @author Mahmoud Ben Hassine
 */
class JobOperatorFailedLaunchIntegrationTests {

	@Test
	void testJobParameterWithoutConverterShouldNotLeaveJobInstanceBehind() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class)) {
			// given
			Job job = context.getBean("job", Job.class);
			JobOperator jobOperator = context.getBean(JobOperator.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
			JobParameters jobParameters = new JobParameters(
					Set.of(new JobParameter<>("customer", new CustomerId("42"), CustomerId.class, true)));

			// when
			Assertions.assertThrows(ConverterNotFoundException.class, () -> jobOperator.start(job, jobParameters));

			// then
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_EXECUTION"));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_INSTANCE"),
					"A failed launch should not leave an orphan job instance behind");
		}
	}

	@Test
	void testInvalidJobParametersShouldNotLeaveJobInstanceBehind() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class)) {
			// given
			Job job = context.getBean("jobWithRejectingValidator", Job.class);
			JobOperator jobOperator = context.getBean(JobOperator.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
			JobParameters jobParameters = new JobParameters(Set.of(new JobParameter<>("name", "foo", String.class)));

			// when
			Assertions.assertThrows(InvalidJobParametersException.class, () -> jobOperator.start(job, jobParameters));

			// then
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_EXECUTION"));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_INSTANCE"),
					"A failed launch should not leave an orphan job instance behind");
		}
	}

	@Test
	void testCreateJobInstanceAndExecutionInRepositoryShouldBeAtomic() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class)) {
			// given
			JobRepository jobRepository = context.getBean(JobRepository.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
			JobParameters jobParameters = new JobParameters(
					Set.of(new JobParameter<>("customer", new CustomerId("42"), CustomerId.class, true)));

			// when
			Assertions.assertThrows(ConverterNotFoundException.class,
					() -> jobRepository.createJobExecution("job", jobParameters));

			// then
			Assertions.assertNull(jobRepository.getJobInstance("job", jobParameters));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_INSTANCE"));
			Assertions.assertEquals(0, count(jdbcTemplate, "BATCH_JOB_EXECUTION"));
		}
	}

	private static int count(JdbcTemplate jdbcTemplate, String table) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
		return count != null ? count : 0;
	}

	/**
	 * A type that is not supported by the conversion service used by default to persist
	 * job parameters: it is neither a built-in type nor does it have a {@code String}
	 * constructor or a static {@code valueOf(String)}/{@code of(String)}/
	 * {@code from(String)} factory method.
	 */
	static class CustomerId {

		private final String value;

		CustomerId(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	@Configuration
	static class TestConfiguration extends JdbcDefaultBatchConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("step", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public Job jobWithRejectingValidator(JobRepository jobRepository, Step step) {
			return new JobBuilder("jobWithRejectingValidator", jobRepository).start(step).validator(parameters -> {
				throw new InvalidJobParametersException("Job parameters are not valid");
			}).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.addScript(DatabaseType.H2.getProductSchema())
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
