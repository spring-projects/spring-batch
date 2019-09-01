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
package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeAnnotatedListenerIntegrationTests {

	@Autowired
	JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void test() {
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("step-under-test");

		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	public static class StatefulItemReader implements ItemReader<String> {

		private List<String> list;

		@BeforeStep
		public void initializeState(StepExecution stepExecution) {
			this.list = new ArrayList<>();
		}

		@AfterStep
		public ExitStatus exploitState(StepExecution stepExecution) {
			System.out.println("******************************");
			System.out.println(" READING RESULTS : " + list.size());

			return stepExecution.getExitStatus();
		}

		@Nullable
		@Override
		public String read() throws Exception {
			this.list.add("some stateful reading information");
			if (list.size() < 10) {
				return "value " + list.size();
			}
			return null;
		}
	}

	@Configuration
	@EnableBatchProcessing
	public static class TestConfig {
		@Autowired
		private JobBuilderFactory jobBuilder;
		@Autowired
		private StepBuilderFactory stepBuilder;

		@Bean
		JobLauncherTestUtils jobLauncherTestUtils() {
			return new JobLauncherTestUtils();
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
		public Job jobUnderTest() {
			return jobBuilder.get("job-under-test")
					.start(stepUnderTest())
					.build();
		}

		@Bean
		public Step stepUnderTest() {
			return stepBuilder.get("step-under-test")
					.<String, String>chunk(1)
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
			return new ItemProcessor<String, String>() {

				@Nullable
				@Override
				public String process(String item) throws Exception {
					return item;
				}
			};
		}

		@Bean
		public ItemWriter<String> writer() {
			return new ItemWriter<String>() {

				@Override
				public void write(List<? extends String> items)
						throws Exception {
				}
			};
		}
	}
}
