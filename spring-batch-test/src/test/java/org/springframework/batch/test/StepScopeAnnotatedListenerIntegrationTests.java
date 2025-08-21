/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig
class StepScopeAnnotatedListenerIntegrationTests {

	@Autowired
	JobOperatorTestUtils jobOperatorTestUtils;

	@Test
	void test(@Autowired Job job) {
		// given
		this.jobOperatorTestUtils.setJob(job);

		// when
		JobExecution jobExecution = jobOperatorTestUtils.startStep("step-under-test");

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	static class StatefulItemReader implements ItemReader<String> {

		private List<String> list;

		@BeforeStep
		public void initializeState(StepExecution stepExecution) {
			this.list = new ArrayList<>();
		}

		@AfterStep
		public ExitStatus exploitState(StepExecution stepExecution) {
			return stepExecution.getExitStatus();
		}

		@Override
		public @Nullable String read() throws Exception {
			this.list.add("some stateful reading information");
			if (list.size() < 10) {
				return "value " + list.size();
			}
			return null;
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class TestConfig {

		@Autowired
		private PlatformTransactionManager transactionManager;

		@Bean
		JobOperatorTestUtils jobOperatorTestUtils(JobRepository jobRepository, JobOperator jobOperator) {
			return new JobOperatorTestUtils(jobOperator, jobRepository);
		}

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
			return embeddedDatabaseBuilder.addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
				.setType(EmbeddedDatabaseType.HSQL)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public Job jobUnderTest(JobRepository jobRepository) {
			return new JobBuilder("job-under-test", jobRepository).start(stepUnderTest(jobRepository)).build();
		}

		@Bean
		public Step stepUnderTest(JobRepository jobRepository) {
			return new StepBuilder("step-under-test", jobRepository).<String, String>chunk(1, this.transactionManager)
				.reader(reader())
				.processor(processor())
				.writer(writer())
				.build();
		}

		@Bean
		@StepScope
		public StatefulItemReader reader() {
			return new StatefulItemReader();
		}

		@Bean
		public ItemProcessor<String, String> processor() {
			return new ItemProcessor<>() {

				@Override
				public @Nullable String process(String item) throws Exception {
					return item;
				}
			};
		}

		@Bean
		public ItemWriter<String> writer() {
			return items -> {
			};
		}

	}

}
