/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;

/**
 * @author Mahmoud Ben Hassine
 */
public class JobBuilderTests {

	@Test
	public void testListeners() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		assertEquals(1, AnnotationBasedJobExecutionListener.beforeJobCount);
		assertEquals(1, AnnotationBasedJobExecutionListener.afterJobCount);
		assertEquals(1, InterfaceBasedJobExecutionListener.beforeJobCount);
		assertEquals(1, InterfaceBasedJobExecutionListener.afterJobCount);

	}

	@Configuration
	@EnableBatchProcessing
	static class MyJobConfiguration {
		@Bean
		public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get("job")
					.listener(new InterfaceBasedJobExecutionListener())
					.listener(new AnnotationBasedJobExecutionListener())
					.start(steps.get("step")
							.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
							.build())
					.build();
		}
	}
	
	static class InterfaceBasedJobExecutionListener implements JobExecutionListener {

		public static int beforeJobCount = 0;
		public static int afterJobCount = 0;

		@Override
		public void beforeJob(JobExecution jobExecution) {
			beforeJobCount++;
		}

		@Override
		public void afterJob(JobExecution jobExecution) {
			afterJobCount++;
		}
	}

	static class AnnotationBasedJobExecutionListener {

		public static int beforeJobCount = 0;
		public static int afterJobCount = 0;
		
		@BeforeJob
		public void beforeJob(JobExecution jobExecution) {
			beforeJobCount++;
		}

		@AfterJob
		public void afterJob(JobExecution jobExecution) {
			afterJobCount++;
		}
	}

}