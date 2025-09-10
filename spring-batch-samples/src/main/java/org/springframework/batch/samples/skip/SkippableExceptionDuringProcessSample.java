/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.samples.skip;

import java.util.Arrays;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class SkippableExceptionDuringProcessSample {

	private final PlatformTransactionManager transactionManager;

	public SkippableExceptionDuringProcessSample(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Bean
	public ItemReader<Integer> itemReader() {
		return new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5, 6)) {
			@Override
			public Integer read() {
				Integer item = super.read();
				System.out.println("reading item = " + item);
				return item;
			}
		};
	}

	@Bean
	public ItemProcessor<Integer, Integer> itemProcessor() {
		return item -> {
			if (item.equals(5)) {
				System.out.println("Throwing exception on item " + item);
				throw new IllegalArgumentException("Unable to process 5");
			}
			System.out.println("processing item = " + item);
			return item;
		};
	}

	@Bean
	public ItemWriter<Integer> itemWriter() {
		return items -> {
			System.out.println("About to write chunk: " + items);
			for (Integer item : items) {
				System.out.println("writing item = " + item);
			}
		};
	}

	@Bean
	public Step step(JobRepository jobRepository) {
		return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(3).transactionManager(this.transactionManager)
			.reader(itemReader())
			.processor(itemProcessor())
			.writer(itemWriter())
			.faultTolerant()
			.skip(IllegalArgumentException.class)
			.skipLimit(3)
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository) {
		return new JobBuilder("job", jobRepository).start(step(jobRepository)).build();
	}

}
