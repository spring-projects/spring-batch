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

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Configuration class that defines a partitioned step based on a
 * {@link VirtualThreadTaskExecutor}.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class JobConfigurationForRunningPartitionedStepsWithVirtualThreads {

	@Bean
	public Step managerStep(JobRepository jobRepository, Step workerStep, Partitioner partitioner) {
		return new StepBuilder("managerStep", jobRepository).partitioner(workerStep.getName(), partitioner)
			.step(workerStep)
			.gridSize(4)
			.taskExecutor(new VirtualThreadTaskExecutor("spring-batch-"))
			.build();
	}

	@Bean
	public Step workerStep(JobRepository jobRepository, JdbcTransactionManager transactionManager, Tasklet tasklet) {
		return new StepBuilder("workerStep", jobRepository).tasklet(tasklet, transactionManager).build();
	}

	@Bean
	@StepScope
	public Tasklet tasklet(@Value("#{stepExecutionContext['data']}") String partitionData) {
		return (contribution, chunkContext) -> {
			System.out.println(Thread.currentThread() + ": processing partition " + partitionData);
			return RepeatStatus.FINISHED;
		};
	}

	@Bean
	public Partitioner partitioner() {
		return gridSize -> {
			Map<String, ExecutionContext> partitionMap = new HashMap<>(gridSize);
			for (int i = 0; i < gridSize; i++) {
				ExecutionContext executionContext = new ExecutionContext();
				executionContext.put("data", "data" + i);
				String key = "partition" + i;
				partitionMap.put(key, executionContext);
			}
			return partitionMap;
		};
	}

	@Bean
	public Job job(JobRepository jobRepository, Step managerStep) {
		return new JobBuilder("job", jobRepository).start(managerStep).build();
	}

}