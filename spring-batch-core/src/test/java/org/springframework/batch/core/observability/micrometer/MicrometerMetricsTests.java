/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.observability.micrometer;

import java.util.List;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrometerMetricsTests {

	@Test
	void testMicrometerMetrics() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
		int expectedJobMetricsCount = 2;

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		List<Meter> meters = meterRegistry.getMeters();
		assertTrue(meters.size() >= expectedJobMetricsCount);
		assertDoesNotThrow(() -> meterRegistry.get(BatchMetrics.METRICS_PREFIX + "job.launch.count").timer(),
				"There should be a meter of type TIMER named spring.batch.job.launch.count registered in the meter registry");
		assertEquals(1, meterRegistry.get(BatchMetrics.METRICS_PREFIX + "job.launch.count").timer().count());
		assertDoesNotThrow(
				() -> meterRegistry.get(BatchMetrics.METRICS_PREFIX + "job")
					.tag(BatchMetrics.METRICS_PREFIX + "job.name", "job")
					.tag(BatchMetrics.METRICS_PREFIX + "job.status", "COMPLETED")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.job registered in the meter registry");

	}

	@Configuration
	@EnableBatchProcessing
	static class MyJobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
					.build())
				.build();
		}

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
			ObservationRegistry observationRegistry = ObservationRegistry.create();
			observationRegistry.observationConfig()
				.observationHandler(new DefaultMeterObservationHandler(meterRegistry));
			return observationRegistry;
		}

	}

}