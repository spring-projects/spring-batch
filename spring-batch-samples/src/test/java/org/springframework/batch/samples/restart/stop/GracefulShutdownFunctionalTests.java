/*
 * Copyright 2006-present the original author or authors.
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

package org.springframework.batch.samples.restart.stop;

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional test for graceful shutdown. A batch container is started in a new thread,
 * then it's stopped using {@link JobOperator#stop}.
 *
 * @author Lucas Ward
 * @author Parikshit Dutta
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 *
 */
@ExtendWith(SpringExtension.class)
class GracefulShutdownFunctionalTests {

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Test
	void testStopJob(@Autowired Job job, @Autowired JobOperator jobOperator) throws Exception {
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		Thread.sleep(1000);

		assertEquals(BatchStatus.STARTED, jobExecution.getStatus());
		assertTrue(jobExecution.isRunning());

		jobOperator.stop(jobExecution);

		int count = 0;
		while (jobExecution.isRunning() && count <= 10) {
			logger.info("Checking for end time in JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		assertFalse(jobExecution.isRunning(), "Timed out waiting for job to end.");
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
	}

	@Configuration
	@EnableBatchProcessing(taskExecutorRef = "batchTaskExecutor")
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step chunkOrientedStep(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 2)
				.reader(new ListItemReader<>(List.of("item1", "item2", "item3", "item4", "item5")))
				.processor(item -> {
					// Simulate processing time
					Thread.sleep(500);
					return item.toUpperCase();
				})
				.writer(new ListItemWriter<>())
				.transactionManager(transactionManager)
				.build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public AsyncTaskExecutor batchTaskExecutor() {
			SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("batch-executor");
			asyncTaskExecutor.setConcurrencyLimit(1);
			return asyncTaskExecutor;
		}

	}

}
