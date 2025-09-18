/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.batch.core.observability;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
class BatchMetricsTests {

	@Test
	void testCalculateDuration() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = startTime.plus(2, ChronoUnit.HOURS)
			.plus(31, ChronoUnit.MINUTES)
			.plus(12, ChronoUnit.SECONDS)
			.plus(42, ChronoUnit.MILLIS);

		Duration duration = BatchMetrics.calculateDuration(startTime, endTime);
		Duration expectedDuration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		assertEquals(expectedDuration, duration);
	}

	@Test
	void testCalculateDurationWhenNoStartTime() {
		Duration duration = BatchMetrics.calculateDuration(null, LocalDateTime.now());
		assertNull(duration);
	}

	@Test
	void testCalculateDurationWhenNoEndTime() {
		Duration duration = BatchMetrics.calculateDuration(LocalDateTime.now(), null);
		assertNull(duration);
	}

	@Test
	void testFormatValidDuration() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("2h31m12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutHours() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("31m12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutMinutes() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutSeconds() {
		Duration duration = Duration.ofMillis(42);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("42ms", formattedDuration);
	}

	@Test
	void testFormatNegativeDuration() {
		Duration duration = Duration.ofMillis(-1);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	void testFormatZeroDuration() {
		String formattedDuration = BatchMetrics.formatDuration(Duration.ZERO);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	void testFormatNullDuration() {
		String formattedDuration = BatchMetrics.formatDuration(null);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	void testBatchMetrics() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
		int expectedBatchMetricsCount = 6;

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		List<Meter> meters = meterRegistry.getMeters();
		assertTrue(meters.size() >= expectedBatchMetricsCount);

		// Job metrics

		assertDoesNotThrow(() -> meterRegistry.get("spring.batch.job.launch.count").timer(),
				"There should be a meter of type TIMER named spring.batch.job.launch.count registered in the meter registry");
		assertEquals(1, meterRegistry.get("spring.batch.job.launch.count").timer().count());

		assertDoesNotThrow(
				() -> meterRegistry.get("spring.batch.job")
					.tag("spring.batch.job.name", "job")
					.tag("spring.batch.job.status", "COMPLETED")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.job registered in the meter registry");

		// Step metrics

		assertDoesNotThrow(
				() -> meterRegistry.get("spring.batch.step")
					.tag("spring.batch.step.name", "step")
					.tag("spring.batch.step.job.name", "job")
					.tag("spring.batch.step.status", "COMPLETED")
					.timer(),
				"There should be a meter of type TIMER named spring.batch.step registered in the meter registry");

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
	@EnableJdbcJobRepository
	static class MyJobConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder(jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5)))
				.writer(items -> {
				})
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
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

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
