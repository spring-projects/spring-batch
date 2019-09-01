/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.PooledEmbeddedDataSource;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

public class MapJobRepositoryConfigurationTests {

	JobLauncher jobLauncher;
	JobRepository jobRepository;
	Job job;
	JobExplorer jobExplorer;

	@Test
	public void testRoseyScenario() throws Exception {
		testConfigurationClass(MapRepositoryBatchConfiguration.class);
	}

	@Test
	public void testOneDataSource() throws Exception {
		testConfigurationClass(HsqlBatchConfiguration.class);
	}

	@Test(expected = UnsatisfiedDependencyException.class)
	public void testMultipleDataSources_whenNoneOfThemIsPrimary() throws Exception {
		testConfigurationClass(InvalidBatchConfiguration.class);
	}

	@Test
	public void testMultipleDataSources_whenNoneOfThemIsPrimaryButOneOfThemIsNamed_dataSource_() throws Exception {
		testConfigurationClass(ValidBatchConfigurationWithoutPrimaryDataSource.class);
	}

	@Test
	public void testMultipleDataSources_whenOneOfThemIsPrimary() throws Exception {
		testConfigurationClass(ValidBatchConfigurationWithPrimaryDataSource.class);
	}

	private void testConfigurationClass(Class<?> clazz) throws Exception {
		GenericApplicationContext context = new AnnotationConfigApplicationContext(clazz);
		this.jobLauncher = context.getBean(JobLauncher.class);
		this.jobRepository = context.getBean(JobRepository.class);
		this.job = context.getBean(Job.class);
		this.jobExplorer = context.getBean(JobExplorer.class);

		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		JobExecution repositoryJobExecution = jobRepository.getLastJobExecution(job.getName(), new JobParameters());
		assertEquals(jobExecution.getId(), repositoryJobExecution.getId());
		assertEquals("job", jobExplorer.getJobNames().iterator().next());
		context.close();
	}

	public static class InvalidBatchConfiguration extends HsqlBatchConfiguration {

		@Bean
		DataSource dataSource2() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().setName("badDatabase").build());
		}
	}

	public static class ValidBatchConfigurationWithPrimaryDataSource extends HsqlBatchConfiguration {

		@Primary
		@Bean
		DataSource dataSource2() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
					setName("dataSource2").
					addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
					addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
					build());
		}
	}

	public static class ValidBatchConfigurationWithoutPrimaryDataSource extends HsqlBatchConfiguration {

		@Bean
		DataSource dataSource() { // will be autowired by name
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
					setName("dataSource").
					addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
					addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
					build());
		}
	}

	public static class HsqlBatchConfiguration extends MapRepositoryBatchConfiguration {

		@Bean
		DataSource dataSource1() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
					setName("dataSource1").
				addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
				addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
				build());
		}
	}

	@Component
	@EnableBatchProcessing
	public static class MapRepositoryBatchConfiguration {
		@Autowired
		JobBuilderFactory jobFactory;

		@Autowired
		StepBuilderFactory stepFactory;

		@Bean
		Step step1 () {
			return stepFactory.get("step1").tasklet(new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					return RepeatStatus.FINISHED;
				}
			}).build();
		}

		@Bean
		Job job() {
			return jobFactory.get("job").start(step1()).build();
		}
	}
}
