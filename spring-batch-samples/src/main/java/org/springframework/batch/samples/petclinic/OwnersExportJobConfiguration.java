/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.batch.samples.petclinic;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class OwnersExportJobConfiguration {

	@Bean
	public JdbcCursorItemReader<Owner> ownersReader(DataSource dataSource) {
		return new JdbcCursorItemReaderBuilder<Owner>().name("ownersReader")
			.sql("SELECT * FROM OWNERS")
			.dataSource(dataSource)
			.rowMapper(new DataClassRowMapper<>(Owner.class))
			.build();
	}

	@Bean
	public FlatFileItemWriter<Owner> ownersWriter() {
		return new FlatFileItemWriterBuilder<Owner>().name("ownersWriter")
			.resource(new FileSystemResource("owners.csv"))
			.delimited()
			.names("id", "firstname", "lastname", "address", "city", "telephone")
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			JdbcCursorItemReader<Owner> ownersReader, FlatFileItemWriter<Owner> ownersWriter) {
		return new JobBuilder("ownersExportJob", jobRepository)
			.start(new StepBuilder("ownersExportStep", jobRepository).<Owner, Owner>chunk(5)
				.transactionManager(transactionManager)
				.reader(ownersReader)
				.writer(ownersWriter)
				.build())
			.build();
	}

}
