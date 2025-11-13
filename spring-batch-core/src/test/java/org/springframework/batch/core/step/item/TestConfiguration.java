/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.item;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class TestConfiguration {

	@Bean
	@StepScope
	public FlatFileItemReader<Person> itemReader(@Value("#{jobParameters['file']}") Resource file) {
		return new FlatFileItemReaderBuilder<Person>().name("personItemReader")
			.resource(file)
			.delimited()
			.names("id", "name")
			.targetType(Person.class)
			.build();
	}

	@Bean
	public ItemProcessor<Person, Person> itemProcessor() {
		return item -> {
			if (System.getProperty("fail") != null) {
				throw new Exception("Unable to process item " + item);
			}
			return new Person(item.id(), item.name().toUpperCase());
		};
	}

	@Bean
	public JdbcBatchItemWriter<Person> itemWriter() {
		String sql = "insert into person_target (id, name) values (:id, :name)";
		return new JdbcBatchItemWriterBuilder<Person>().dataSource(dataSource()).sql(sql).beanMapped().build();
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
			.addScript("/org/springframework/batch/core/schema-h2.sql")
			.addScript("schema.sql")
			.generateUniqueName(true)
			.build();
	}

	@Bean
	public JdbcTransactionManager transactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

}