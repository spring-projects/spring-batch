/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.samples.partitioning.local;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.samples.common.ColumnRangePartitioner;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.DataClassRowMapper;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class PartitionJdbcJobConfiguration {

	public record Owner(int id, String firstName, String lastName) {
	}

	@Bean
	@StepScope
	public JdbcCursorItemReader<Owner> ownersReader(DataSource dataSource,
			@Value("#{stepExecutionContext['minValue']}") int minValue,
			@Value("#{stepExecutionContext['maxValue']}") int maxValue) {
		String query = String.format("SELECT * FROM OWNERS WHERE ID BETWEEN %s AND %s", minValue, maxValue);
		return new JdbcCursorItemReaderBuilder<Owner>().name("ownersReader")
			.sql(query)
			.dataSource(dataSource)
			.rowMapper(new DataClassRowMapper<>(Owner.class))
			.build();
	}

	@Bean
	public Step partitionedStep(JobRepository jobRepository, DataSource dataSource,
			JdbcCursorItemReader<Owner> ownersReader) {
		Step workerStep = new StepBuilder("workerStep", jobRepository).<Owner, Owner>chunk(3)
			.reader(ownersReader)
			.writer(chunk -> System.out
				.println(Thread.currentThread().getName() + " - writing chunk: " + chunk.getItems()))
			.build();

		ColumnRangePartitioner ownersPartitioner = new ColumnRangePartitioner();
		ownersPartitioner.setColumn("ID");
		ownersPartitioner.setDataSource(dataSource);
		ownersPartitioner.setTable("OWNERS");
		return new StepBuilder(jobRepository).partitioner("workerStep", ownersPartitioner)
			.step(workerStep)
			.gridSize(2)
			.taskExecutor(new SimpleAsyncTaskExecutor())
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, Step partitionedStep) {
		return new JobBuilder(jobRepository).start(partitionedStep).build();
	}

}