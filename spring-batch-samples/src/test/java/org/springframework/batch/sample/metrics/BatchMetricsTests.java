/*
 * Copyright 2019 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.hamcrest.Matchers;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class BatchMetricsTests {

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
		assertThat(meters, Matchers.hasSize(7));

		Meter.Id itemReadMeter = meters.get(0).getId();
		assertEquals(itemReadMeter.getName(), "spring.batch.item.read");
		assertEquals(itemReadMeter.getDescription(), "Item reading duration in seconds");
		assertEquals(itemReadMeter.getTag("job.name"), "job");
		assertEquals(itemReadMeter.getTag("step.name"), "step2");
		assertEquals(itemReadMeter.getTag("status"), "SUCCESS");
		assertEquals(itemReadMeter.getType(), Meter.Type.TIMER);

		Meter.Id step1Meter = meters.get(1).getId();
		assertEquals(step1Meter.getName(), "spring.batch.step");
		assertEquals(step1Meter.getDescription(), "Step duration in seconds");
		assertEquals(step1Meter.getTag("name"), "step1");
		assertEquals(step1Meter.getTag("job.name"), "job");
		assertEquals(step1Meter.getTag("status"), "COMPLETED");
		assertEquals(step1Meter.getType(), Meter.Type.TIMER);

		Meter.Id step2Meter = meters.get(2).getId();
		assertEquals(step2Meter.getName(), "spring.batch.step");
		assertEquals(step2Meter.getDescription(), "Step duration in seconds");
		assertEquals(step2Meter.getTag("name"), "step2");
		assertEquals(step2Meter.getTag("job.name"), "job");
		assertEquals(step2Meter.getTag("status"), "COMPLETED");
		assertEquals(step2Meter.getType(), Meter.Type.TIMER);

		Meter.Id activeJobsMeter = meters.get(3).getId();
		assertEquals(activeJobsMeter.getName(), "spring.batch.jobs.active");
		assertEquals(activeJobsMeter.getDescription(), "Active jobs");
		assertEquals(activeJobsMeter.getTags().size(), 0);
		assertEquals(activeJobsMeter.getType(), Meter.Type.LONG_TASK_TIMER);

		Meter.Id chunkWriteMeter = meters.get(4).getId();
		assertEquals(chunkWriteMeter.getName(), "spring.batch.chunk.write");
		assertEquals(chunkWriteMeter.getDescription(), "Chunk writing duration in seconds");
		assertEquals(chunkWriteMeter.getTag("job.name"), "job");
		assertEquals(chunkWriteMeter.getTag("step.name"), "step2");
		assertEquals(chunkWriteMeter.getTag("status"), "SUCCESS");
		assertEquals(chunkWriteMeter.getType(), Meter.Type.TIMER);

		Meter.Id itemProcessMeter = meters.get(5).getId();
		assertEquals(itemProcessMeter.getName(), "spring.batch.item.process");
		assertEquals(itemProcessMeter.getDescription(), "Item processing duration in seconds");
		assertEquals(itemProcessMeter.getTag("job.name"), "job");
		assertEquals(itemProcessMeter.getTag("step.name"), "step2");
		assertEquals(itemProcessMeter.getTag("status"), "SUCCESS");
		assertEquals(itemProcessMeter.getType(), Meter.Type.TIMER);

		Meter.Id jobMeter = meters.get(6).getId();
		assertEquals(jobMeter.getName(), "spring.batch.job");
		assertEquals(jobMeter.getDescription(), "Job duration in seconds");
		assertEquals(jobMeter.getTag("name"), "job");
		assertEquals(jobMeter.getTag("status"), "COMPLETED");
		assertEquals(jobMeter.getType(), Meter.Type.TIMER);
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
		public ItemReader<Integer> itemReader() {
			return new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		}

		@Bean
		public ItemWriter<Integer> itemWriter() {
			return items -> {
				for (Integer item : items) {
					System.out.println("item = " + item);
				}
			};
		}

		@Bean
		public Step step2() {
			return stepBuilderFactory.get("step2")
					.<Integer, Integer>chunk(5)
					.reader(itemReader())
					.writer(itemWriter())
					.build();
		}

		@Bean
		public Job job() {
			return jobBuilderFactory.get("job")
					.start(step1())
					.next(step2())
					.build();
		}
	}
}
