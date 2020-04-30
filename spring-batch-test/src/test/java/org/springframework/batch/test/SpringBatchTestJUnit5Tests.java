/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.test;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test cases for usage of {@link SpringBatchTest} annotation with JUnit 5.
 *
 * @author Mahmoud Ben Hassine
 */
@SpringBatchTest
@ContextConfiguration
public class SpringBatchTestJUnit5Tests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private ItemReader<String> stepScopedItemReader;

	@Autowired
	private ItemReader<String> jobScopedItemReader;

	@BeforeEach
	public void setUp() {
		this.jobRepositoryTestUtils.removeJobExecutions();
	}

	@Test
	public void testStepScopedItemReader() throws Exception {
		Assertions.assertEquals("foo", this.stepScopedItemReader.read());
		Assertions.assertEquals("bar", this.stepScopedItemReader.read());
		Assertions.assertNull(this.stepScopedItemReader.read());
	}

	@Test
	public void testJobScopedItemReader() throws Exception {
		Assertions.assertEquals("foo", this.jobScopedItemReader.read());
		Assertions.assertEquals("bar", this.jobScopedItemReader.read());
		Assertions.assertNull(this.jobScopedItemReader.read());
	}

	@Test
	public void testJob() throws Exception {
		// given
		JobParameters jobParameters = this.jobLauncherTestUtils.getUniqueJobParameters();
		
		// when
		JobExecution jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	public StepExecution getStepExecution() {
		StepExecution execution = MetaDataInstanceFactory.createStepExecution();
		execution.getExecutionContext().putString("input.data", "foo,bar");
		return execution;
	}

	public JobExecution getJobExecution() {
		JobExecution execution = MetaDataInstanceFactory.createJobExecution();
		execution.getExecutionContext().putString("input.data", "foo,bar");
		return execution;
	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.HSQL)
					.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
					.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
					.build();
		}

		@Bean
		@StepScope
		public ItemReader<String> stepScopedItemReader(@Value("#{stepExecutionContext['input.data']}") String data) {
			return new ListItemReader<>(Arrays.asList(data.split(",")));
		}

		@Bean
		@JobScope
		public ItemReader<String> jobScopedItemReader(@Value("#{jobExecutionContext['input.data']}") String data) {
			return new ListItemReader<>(Arrays.asList(data.split(",")));
		}

		@Bean
		public Job job(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
			return jobBuilderFactory.get("job")
					.start(stepBuilderFactory.get("step")
							.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
							.build())
					.build();
		}
	}
}
