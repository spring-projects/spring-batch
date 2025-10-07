/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Configuration class that defines a parallel flow of steps based on a
 * {@link VirtualThreadTaskExecutor}.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class JobConfigurationForRunningParallelStepsWithVirtualThreads {

	@Bean
	public Step step1(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return createStep("step1", jobRepository, transactionManager);
	}

	@Bean
	public Step step2(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return createStep("step2", jobRepository, transactionManager);
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step1, Step step2) {
		Flow flow1 = new FlowBuilder<Flow>("subflow1").from(step1).end();
		Flow flow2 = new FlowBuilder<Flow>("subflow2").from(step2).end();

		Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(new VirtualThreadTaskExecutor("spring-batch-"))
			.add(flow1, flow2)
			.build();

		return new JobBuilder("job", jobRepository).start(splitFlow).build().build();
	}

	private Step createStep(String stepName, JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return new StepBuilder(stepName, jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println(Thread.currentThread() + ": running " + stepName);
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

}