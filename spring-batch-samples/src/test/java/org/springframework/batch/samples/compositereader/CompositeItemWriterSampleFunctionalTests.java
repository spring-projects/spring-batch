/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.samples.compositereader;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.jdbc.JdbcTestUtils;

public class CompositeItemWriterSampleFunctionalTests {

	record Person(int id, String name) {
	}

	@Test
	void testJobLaunch() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
		int personsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target");
		Assertions.assertEquals(6, personsCount);
	}

	@Configuration
	@EnableBatchProcessing
	static class JobConfiguration {

		@Bean
		public FlatFileItemReader<Person> itemReader1() {
			return new FlatFileItemReaderBuilder<Person>().name("personItemReader1")
				.resource(new ClassPathResource("org/springframework/batch/samples/compositereader/data/persons1.csv"))
				.delimited()
				.names("id", "name")
				.targetType(Person.class)
				.build();
		}

		@Bean
		public FlatFileItemReader<Person> itemReader2() {
			return new FlatFileItemReaderBuilder<Person>().name("personItemReader2")
				.resource(new ClassPathResource("org/springframework/batch/samples/compositereader/data/persons2.csv"))
				.delimited()
				.names("id", "name")
				.targetType(Person.class)
				.build();
		}

		@Bean
		public JdbcCursorItemReader<Person> itemReader3() {
			String sql = "select * from person_source";
			return new JdbcCursorItemReaderBuilder<Person>().name("personItemReader3")
				.dataSource(dataSource())
				.sql(sql)
				.rowMapper(new DataClassRowMapper<>(Person.class))
				.build();
		}

		@Bean
		public CompositeItemReader<Person> itemReader() {
			return new CompositeItemReader<>(Arrays.asList(itemReader1(), itemReader2(), itemReader3()));
		}

		@Bean
		public JdbcBatchItemWriter<Person> itemWriter() {
			String sql = "insert into person_target (id, name) values (:id, :name)";
			return new JdbcBatchItemWriterBuilder<Person>().dataSource(dataSource()).sql(sql).beanMapped().build();
		}

		@Bean
		public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step", jobRepository).<Person, Person>chunk(5, transactionManager)
					.reader(itemReader())
					.writer(itemWriter())
					.build())
				.build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.addScript("/org/springframework/batch/samples/compositereader/sql/schema.sql")
				.addScript("/org/springframework/batch/samples/compositereader/sql/data.sql")
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}