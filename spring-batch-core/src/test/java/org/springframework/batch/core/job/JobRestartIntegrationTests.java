/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.job;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class JobRestartIntegrationTests {

	@Test
	void testJobRestartWithNoIdentifyingJobParameters(@Autowired JobOperator jobOperator, @Autowired Job job)
			throws JobInstanceAlreadyCompleteException, InvalidJobParametersException,
			JobExecutionAlreadyRunningException, JobRestartException {
		// given
		JobParameters jobParameters1 = new JobParametersBuilder().addString("name", "foo", false).toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString("name", "bar", false).toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters1);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertThrows(JobInstanceAlreadyCompleteException.class,
				() -> jobOperator.start(job, jobParameters2));
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository) {
			Step step = new StepBuilder("step", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
				.build();
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
