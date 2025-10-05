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

import java.util.Arrays;
import java.util.concurrent.Future;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Configuration class that defines a chunk-oriented step with an asynchronous item
 * processor based on a {@link VirtualThreadTaskExecutor}.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class JobConfigurationForAsynchronousItemProcessingWithVirtualThreads {

	@Bean
	public ItemReader<Integer> itemReader() {
		return new ListItemReader<>(Arrays.asList(0, 1, 2, 3, 4, 5));
	}

	@Bean
	public AsyncItemProcessor<Integer, Integer> itemProcessor() {
		ItemProcessor<Integer, Integer> delegate = item -> {
			System.out.println(Thread.currentThread() + ": processing item " + item);
			return item + 1;
		};
		AsyncItemProcessor<Integer, Integer> asyncItemProcessor = new AsyncItemProcessor<>(delegate);
		asyncItemProcessor.setTaskExecutor(new VirtualThreadTaskExecutor("spring-batch-"));
		return asyncItemProcessor;
	}

	@Bean
	public AsyncItemWriter<Integer> itemWriter() {
		ItemWriter<Integer> delegate = items -> {
			for (Integer item : items) {
				System.out.println(Thread.currentThread() + ": writing item " + item);
			}
		};

		return new AsyncItemWriter<>(delegate);
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			ItemReader<Integer> itemReader, AsyncItemProcessor<Integer, Integer> itemProcessor,
			AsyncItemWriter<Integer> itemWriter) {
		Step step = new StepBuilder("step", jobRepository).<Integer, Future<Integer>>chunk(2)
			.transactionManager(transactionManager)
			.reader(itemReader)
			.processor(itemProcessor)
			.writer(itemWriter)
			.build();
		return new JobBuilder("job", jobRepository).start(step).build();
	}

}