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
package org.springframework.batch.samples.shutdown;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.JacksonExecutionContextStringSerializer;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Sample batch configuration for demonstrating job shutdown and restart.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@PropertySource("classpath:org/springframework/batch/samples/shutdown/application.properties")
@EnableBatchProcessing(taskExecutorRef = "batchTaskExecutor")
@EnableJdbcJobRepository
class JobConfiguration {

	record Vet(int id, String firstname, String lastname) {
	}

	@Bean
	@StepScope
	public JdbcCursorItemReader<Vet> vetsReader(DataSource dataSource, @Value("#{jobParameters['minId']}") long minId,
			@Value("#{jobParameters['maxId']}") long maxId) {
		String sql = "select * from vets_in where id >= " + minId + " and id <= " + maxId + " order by id";
		return new JdbcCursorItemReaderBuilder<Vet>().name("vetsReader")
			.dataSource(dataSource)
			.sql(sql)
			.rowMapper(new DataClassRowMapper<>(Vet.class))
			.build();
	}

	@Bean
	public JdbcBatchItemWriter<Vet> vetsWriter(DataSource dataSource) {
		String sql = "insert into vets_out (id, firstname, lastname) values (:id, :firstname, :lastname)";
		return new JdbcBatchItemWriterBuilder<Vet>().dataSource(dataSource).sql(sql).beanMapped().build();
	}

	@Bean
	public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			JdbcCursorItemReader<Vet> vetsReader, JdbcBatchItemWriter<Vet> vetsWriter) {
		return new StepBuilder("step", jobRepository).<Vet, Vet>chunk(2)
			.transactionManager(transactionManager)
			.reader(vetsReader)
			.processor(vet -> {
				Thread.sleep(5000); // simulate slow processing
				System.out.println("Processing vet " + vet);
				return new Vet(vet.id, vet.firstname.toUpperCase(), vet.lastname.toUpperCase());
			})
			.writer(vetsWriter)
			.build();
	}

	@Bean
	public Job Job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

	// infrastructure beans
	@Bean
	public DataSource dataSource(Environment environment) {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUrl(environment.getProperty("spring.datasource.url"));
		dataSource.setUser(environment.getProperty("spring.datasource.username"));
		dataSource.setPassword(environment.getProperty("spring.datasource.password"));
		return dataSource;
	}

	@Bean
	public JdbcTransactionManager transactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

	@Bean
	public ExecutionContextSerializer executionContextSerializer() {
		return new JacksonExecutionContextStringSerializer();
	}

	// run jobs in background threads to allow the main thread to continue
	// and register shutdown hooks
	@Bean
	public TaskExecutor batchTaskExecutor() {
		ThreadPoolTaskExecutor batchTaskExecutor = new ThreadPoolTaskExecutor();
		batchTaskExecutor.setCorePoolSize(1);
		batchTaskExecutor.setMaxPoolSize(10); // max of 10 parallel jobs at a time
		batchTaskExecutor.setThreadNamePrefix("batch-");
		return batchTaskExecutor;
	}

}