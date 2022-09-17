/*
 * Copyright 2019-2022 the original author or authors.
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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

	private static final int EXPECTED_SPRING_BATCH_METRICS = 10;

	@Test
	void testCalculateDuration() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = startTime.plus(2, ChronoUnit.HOURS).plus(31, ChronoUnit.MINUTES)
				.plus(12, ChronoUnit.SECONDS).plus(42, ChronoUnit.MILLIS);

		Duration duration = BatchMetrics.calculateDuration(toDate(startTime), toDate(endTime));
		Duration expectedDuration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		assertEquals(expectedDuration, duration);
	}

	@Test
	void testCalculateDurationWhenNoStartTime() {
		Duration duration = BatchMetrics.calculateDuration(null, toDate(LocalDateTime.now()));
		assertNull(duration);
	}

	@Test
	void testCalculateDurationWhenNoEndTime() {
		Duration duration = BatchMetrics.calculateDuration(toDate(LocalDateTime.now()), null);
		assertNull(duration);
	}

	private Date toDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
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
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		List<Meter> meters = Metrics.globalRegistry.getMeters();
		assertTrue(meters.size() >= EXPECTED_SPRING_BATCH_METRICS);

		// Job metrics

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.job").tag("spring.batch.job.name", "job")
						.tag("spring.batch.job.status", "COMPLETED").timer(),
				"There should be a meter of type TIMER named spring.batch.job registered in the global registry");

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.job.active").tag("spring.batch.job.active.name", "job")
						.longTaskTimer(),
				"There should be a meter of type LONG_TASK_TIMER named spring.batch.job.active"
						+ " registered in the global registry");

		// Step 1 (tasklet) metrics

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.step").tag("spring.batch.step.name", "step1")
						.tag("spring.batch.step.job.name", "job").tag("spring.batch.step.status", "COMPLETED").timer(),
				"There should be a meter of type TIMER named spring.batch.step registered in the global registry");

		// Step 2 (simple chunk-oriented) metrics

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.step").tag("spring.batch.step.name", "step2")
						.tag("spring.batch.step.job.name", "job").tag("spring.batch.step.status", "COMPLETED").timer(),
				"There should be a meter of type TIMER named spring.batch.step registered in the global registry");

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.item.read").tag("spring.batch.item.read.job.name", "job")
						.tag("spring.batch.item.read.step.name", "step2")
						.tag("spring.batch.item.read.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.item.read registered in the global registry");

		assertDoesNotThrow(() -> Metrics.globalRegistry.get("spring.batch.item.process")
				.tag("spring.batch.item.process.job.name", "job").tag("spring.batch.item.process.step.name", "step2")
				.tag("spring.batch.item.process.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.item.process registered in the global registry");

		assertDoesNotThrow(() -> Metrics.globalRegistry.get("spring.batch.chunk.write")
				.tag("spring.batch.chunk.write.job.name", "job").tag("spring.batch.chunk.write.step.name", "step2")
				.tag("spring.batch.chunk.write.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.chunk.write registered in the global registry");

		// Step 3 (fault-tolerant chunk-oriented) metrics

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.step").tag("spring.batch.step.name", "step3")
						.tag("spring.batch.step.job.name", "job").tag("spring.batch.step.status", "COMPLETED").timer(),
				"There should be a meter of type TIMER named spring.batch.step registered in the global registry");

		assertDoesNotThrow(
				() -> Metrics.globalRegistry.get("spring.batch.item.read").tag("spring.batch.item.read.job.name", "job")
						.tag("spring.batch.item.read.step.name", "step3")
						.tag("spring.batch.item.read.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.item.read registered in the global registry");

		assertDoesNotThrow(() -> Metrics.globalRegistry.get("spring.batch.item.process")
				.tag("spring.batch.item.process.job.name", "job").tag("spring.batch.item.process.step.name", "step3")
				.tag("spring.batch.item.process.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.item.process registered in the global registry");

		assertDoesNotThrow(() -> Metrics.globalRegistry.get("spring.batch.chunk.write")
				.tag("spring.batch.chunk.write.job.name", "job").tag("spring.batch.chunk.write.step.name", "step3")
				.tag("spring.batch.chunk.write.status", "SUCCESS").timer(),
				"There should be a meter of type TIMER named spring.batch.chunk.write registered in the global registry");
	}

	@Configuration
	@EnableBatchProcessing
	@Import(DataSoourceConfiguration.class)
	static class MyJobConfiguration {

		private PlatformTransactionManager transactionManager;

		public MyJobConfiguration(PlatformTransactionManager transactionManager) {
			this.transactionManager = transactionManager;
		}

		@Bean
		public Step step1(JobRepository jobRepository) {
			return new StepBuilder("step1", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, this.transactionManager).build();
		}

		@Bean
		public Step step2(JobRepository jobRepository) {
			return new StepBuilder("step2", jobRepository).<Integer, Integer>chunk(2, this.transactionManager)
					.reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5)))
					.writer(items -> items.forEach(System.out::println)).build();
		}

		@Bean
		public Step step3(JobRepository jobRepository) {
			return new StepBuilder("step3", jobRepository).<Integer, Integer>chunk(2, this.transactionManager)
					.reader(new ListItemReader<>(Arrays.asList(6, 7, 8, 9, 10)))
					.writer(items -> items.forEach(System.out::println)).faultTolerant().skip(Exception.class)
					.skipLimit(3).build();
		}

		@Bean
		public Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository).start(step1(jobRepository)).next(step2(jobRepository))
					.next(step3(jobRepository)).build();
		}

	}

	@Configuration
	static class DataSoourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
					.addScript("/org/springframework/batch/core/schema-hsqldb.sql").generateUniqueName(true).build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
