/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.batch.samples.file.delimited;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class DelimitedJobConfiguration {

	@Bean
	@StepScope
	public FlatFileItemReader<CustomerCredit> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
		return new FlatFileItemReaderBuilder<CustomerCredit>().name("itemReader")
			.resource(resource)
			.delimited()
			.names("name", "credit")
			.targetType(CustomerCredit.class)
			.build();
	}

	@Bean
	@StepScope
	public FlatFileItemWriter<CustomerCredit> itemWriter(
			@Value("#{jobParameters[outputFile]}") WritableResource resource) {
		return new FlatFileItemWriterBuilder<CustomerCredit>().name("itemWriter")
			.resource(resource)
			.delimited()
			.names("name", "credit")
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			ItemReader<CustomerCredit> itemReader, ItemWriter<CustomerCredit> itemWriter) {
		return new JobBuilder("ioSampleJob", jobRepository)
			.start(new StepBuilder("step1", jobRepository).<CustomerCredit, CustomerCredit>chunk(2)
				.transactionManager(transactionManager)
				.reader(itemReader)
				.processor(new CustomerCreditIncreaseProcessor())
				.writer(itemWriter)
				.build())
			.build();
	}

}
