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
package org.springframework.batch.core.repository;

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class JobRepositoryIntegrationTests {

	// Issue https://github.com/spring-projects/spring-batch/issues/5104
	@Test
	void testFindRunningJobExecutions(@Autowired JobOperator jobOperator, @Autowired JobRepository jobRepository,
			@Autowired Job job) throws Exception {
		// Start a job execution
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		// Find running job executions, none at this point
		Set<JobExecution> runningExecutions = jobRepository.findRunningJobExecutions(job.getName());

		// then
		Assertions.assertTrue(runningExecutions.isEmpty());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository) {
			Tasklet tasklet = (contribution, chunkContext) -> RepeatStatus.FINISHED;
			TaskletStep step = new StepBuilder("step", jobRepository).tasklet(tasklet).build();
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
				.addScript("/org/springframework/batch/core/schema-h2.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
