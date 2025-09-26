/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.samples.metrics;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class Job2Configuration {

	private final Random random;

	public Job2Configuration() {
		this.random = new Random();
	}

	@Bean
	public Job job2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new JobBuilder("job2", jobRepository).start(step(jobRepository, transactionManager)).build();
	}

	@Bean
	public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("step1", jobRepository).<Integer, Integer>chunk(3)
			.transactionManager(transactionManager)
			.reader(itemReader())
			.writer(itemWriter())
			.build();
	}

	@Bean
	@StepScope
	public ListItemReader<Integer> itemReader() {
		List<Integer> items = new LinkedList<>();
		// read a random number of items in each run
		for (int i = 0; i < random.nextInt(100); i++) {
			items.add(i);
		}
		return new ListItemReader<>(items);
	}

	@Bean
	public ItemWriter<Integer> itemWriter() {
		return items -> {
			for (Integer item : items) {
				int nextInt = random.nextInt(1000);
				Thread.sleep(nextInt);
				// simulate write failure
				if (nextInt % 57 == 0) {
					throw new Exception("Boom!");
				}
				System.out.println("item = " + item);
			}
		};
	}

}
