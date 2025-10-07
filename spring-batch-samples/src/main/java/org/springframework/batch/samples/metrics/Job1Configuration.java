/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Random;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class Job1Configuration {

	private final Random random;

	public Job1Configuration() {
		this.random = new Random();
	}

	@Bean
	public Job job1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new JobBuilder("job1", jobRepository).start(step1(jobRepository, transactionManager))
			.next(step2(jobRepository, transactionManager))
			.build();
	}

	@Bean
	public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("step1", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("hello");
			// simulate processing time
			Thread.sleep(random.nextInt(3000));
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Step step2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("step2", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("world");
			// simulate step failure
			int nextInt = random.nextInt(3000);
			Thread.sleep(nextInt);
			if (nextInt % 5 == 0) {
				throw new Exception("Boom!");
			}
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

}
