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
package org.springframework.batch.core.step.item;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration tests for observability features in {@link ChunkOrientedStep}.
 *
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepObservabilityIntegrationTests {

	@Test
	void testChunkOrientedStepMetrics() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
		SimpleMeterRegistry meterRegistry = context.getBean(SimpleMeterRegistry.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertEquals(3, meterRegistry.getMeters().size());
		assertDoesNotThrow(
				() -> meterRegistry.get("spring.batch.item.read")
					.tag("spring.batch.item.read.job.name", "job")
					.tag("spring.batch.item.read.step.name", "step")
					.tag("spring.batch.item.read.status", "SUCCESS")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.item.read registered in the meter registry");
		assertDoesNotThrow(
				() -> meterRegistry.get("spring.batch.item.process")
					.tag("spring.batch.item.process.job.name", "job")
					.tag("spring.batch.item.process.step.name", "step")
					.tag("spring.batch.item.process.status", "SUCCESS")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.item.process registered in the meter registry");
		assertDoesNotThrow(
				() -> meterRegistry.get("spring.batch.chunk.write")
					.tag("spring.batch.chunk.write.job.name", "job")
					.tag("spring.batch.chunk.write.step.name", "step")
					.tag("spring.batch.chunk.write.status", "SUCCESS")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.chunk.write registered in the meter registry");
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, MeterRegistry meterRegistry) {
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 2)
				.reader(new ListItemReader<>(List.of("one", "two", "three", "four", "five")))
				.processor(String::toUpperCase)
				.writer(items -> {
				})
				.meterRegistry(meterRegistry)
				.build();
		}

		@Bean
		public SimpleMeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}