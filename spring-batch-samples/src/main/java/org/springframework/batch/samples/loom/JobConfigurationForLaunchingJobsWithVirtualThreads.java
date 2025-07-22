/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.batch.samples.loom;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Configuration class that defines a {@link JobOperator} based on a
 * {@link VirtualThreadTaskExecutor}.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class JobConfigurationForLaunchingJobsWithVirtualThreads {

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		Step step = new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> {
			String message = Thread.currentThread() + ": Hello virtual threads world!";
			contribution.getStepExecution().getJobExecution().getExecutionContext().put("message", message);
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
		return new JobBuilder("job", jobRepository).start(step).build();
	}

	@Bean
	public TaskExecutor taskExecutor() {
		return new VirtualThreadTaskExecutor("spring-batch-");
	}

}