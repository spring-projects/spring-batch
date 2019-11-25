/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.batch.sample.skip;

import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
public class SkippableExceptionDuringReadSample {

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;

	public SkippableExceptionDuringReadSample(JobBuilderFactory jobBuilderFactory,
											  StepBuilderFactory stepBuilderFactory) {
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
	}

	@Bean
	public ItemReader<Integer> itemReader() {
		return new ListItemReader<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6)) {
			@Override
			public Integer read() {
				Integer item = super.read();
				System.out.println("reading item = " + item);
				if (item != null && item.equals(5)) {
					System.out.println("Throwing exception on item " + item);
					throw new IllegalArgumentException("Sorry, no 5 here!");
				}
				return item;
			}
		};
	}

	@Bean
	public ItemProcessor<Integer, Integer> itemProcessor() {
		return item -> {
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
	public Step step() {
		return this.stepBuilderFactory.get("step")
				.<Integer, Integer>chunk(3)
				.reader(itemReader())
				.processor(itemProcessor())
				.writer(itemWriter())
				.faultTolerant()
				.skip(IllegalArgumentException.class)
				.skipLimit(3)
				.build();
	}

	@Bean
	public Job job() {
		return this.jobBuilderFactory.get("job")
				.start(step())
				.build();
	}

}
