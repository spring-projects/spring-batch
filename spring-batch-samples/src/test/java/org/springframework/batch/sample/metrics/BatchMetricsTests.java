/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.batch.sample.metrics;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.junit.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.metrics.BatchMetrics;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mahmoud Ben Hassine
 */
public class BatchMetricsTests {

	private static final int EXPECTED_SPRING_BATCH_METRICS = 10;

	@Test
	public void testCalculateDuration() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = startTime
				.plus(2, ChronoUnit.HOURS)
				.plus(31, ChronoUnit.MINUTES)
				.plus(12, ChronoUnit.SECONDS)
				.plus(42, ChronoUnit.MILLIS);

		Duration duration = BatchMetrics.calculateDuration(toDate(startTime), toDate(endTime));
		Duration expectedDuration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		assertEquals(expectedDuration, duration);
	}

	@Test
	public void testCalculateDurationWhenNoStartTime() {
		Duration duration = BatchMetrics.calculateDuration(null, toDate(LocalDateTime.now()));
		assertNull(duration);
	}

	@Test
	public void testCalculateDurationWhenNoEndTime() {
		Duration duration = BatchMetrics.calculateDuration(toDate(LocalDateTime.now()), null);
		assertNull(duration);
	}

	private Date toDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

	@Test
	public void testFormatValidDuration() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("2h31m12s42ms", formattedDuration);
	}

	@Test
	public void testFormatValidDurationWithoutHours() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("31m12s42ms", formattedDuration);
	}

	@Test
	public void testFormatValidDurationWithoutMinutes() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("12s42ms", formattedDuration);
	}

	@Test
	public void testFormatValidDurationWithoutSeconds() {
		Duration duration = Duration.ofMillis(42);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("42ms", formattedDuration);
	}

	@Test
	public void testFormatNegativeDuration() {
		Duration duration = Duration.ofMillis(-1);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	public void testFormatZeroDuration() {
		String formattedDuration = BatchMetrics.formatDuration(Duration.ZERO);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	public void testFormatNullDuration() {
		String formattedDuration = BatchMetrics.formatDuration(null);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	public void testBatchMetrics() throws Exception {
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

		try {
			Metrics.globalRegistry.get("spring.batch.job")
					.tag("name", "job")
					.tag("status", "COMPLETED")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.job " +
					"registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.job.active")
					.tag("name", "job")
					.longTaskTimer();
		} catch (Exception e) {
			fail("There should be a meter of type LONG_TASK_TIMER named spring.batch.job.active" +
					" registered in the global registry: " + e.getMessage());
		}
		
		// Step 1 (tasklet) metrics

		try {
			Metrics.globalRegistry.get("spring.batch.step")
					.tag("name", "step1")
					.tag("job.name", "job")
					.tag("status", "COMPLETED")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.step" +
					" registered in the global registry: " + e.getMessage());
		}
		
		// Step 2 (simple chunk-oriented) metrics

		try {
			Metrics.globalRegistry.get("spring.batch.step")
					.tag("name", "step2")
					.tag("job.name", "job")
					.tag("status", "COMPLETED")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.step" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.item.read")
					.tag("job.name", "job")
					.tag("step.name", "step2")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.item.read" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.item.process")
					.tag("job.name", "job")
					.tag("step.name", "step2")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.item.process" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.chunk.write")
					.tag("job.name", "job")
					.tag("step.name", "step2")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.chunk.write" +
					" registered in the global registry: " + e.getMessage());
		}
		
		// Step 3 (fault-tolerant chunk-oriented) metrics

		try {
			Metrics.globalRegistry.get("spring.batch.step")
					.tag("name", "step3")
					.tag("job.name", "job")
					.tag("status", "COMPLETED")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.step" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.item.read")
					.tag("job.name", "job")
					.tag("step.name", "step3")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.item.read" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.item.process")
					.tag("job.name", "job")
					.tag("step.name", "step3")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.item.process" +
					" registered in the global registry: " + e.getMessage());
		}

		try {
			Metrics.globalRegistry.get("spring.batch.chunk.write")
					.tag("job.name", "job")
					.tag("step.name", "step3")
					.tag("status", "SUCCESS")
					.timer();
		} catch (Exception e) {
			fail("There should be a meter of type TIMER named spring.batch.chunk.write" +
					" registered in the global registry: " + e.getMessage());
		}
	}

	@Configuration
	@EnableBatchProcessing
	static class MyJobConfiguration {

		private JobBuilderFactory jobBuilderFactory;
		private StepBuilderFactory stepBuilderFactory;

		public MyJobConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
			this.jobBuilderFactory = jobBuilderFactory;
			this.stepBuilderFactory = stepBuilderFactory;
		}

		@Bean
		public Step step1() {
			return stepBuilderFactory.get("step1")
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
					.build();
		}

		@Bean
		public Step step2() {
			return stepBuilderFactory.get("step2")
					.<Integer, Integer>chunk(2)
					.reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5)))
					.writer(items -> items.forEach(System.out::println))
					.build();
		}

		@Bean
		public Step step3() {
			return stepBuilderFactory.get("step3")
					.<Integer, Integer>chunk(2)
					.reader(new ListItemReader<>(Arrays.asList(6, 7, 8, 9, 10)))
					.writer(items -> items.forEach(System.out::println))
					.faultTolerant()
					.skip(Exception.class)
					.skipLimit(3)
					.build();
		}

		@Bean
		public Job job() {
			return jobBuilderFactory.get("job")
					.start(step1())
					.next(step2())
					.next(step3())
					.build();
		}
	}
}
