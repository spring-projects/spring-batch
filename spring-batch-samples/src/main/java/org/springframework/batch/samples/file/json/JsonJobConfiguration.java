/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.batch.samples.file.json;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.json.GsonJsonObjectReader;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.domain.trade.Trade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class JsonJobConfiguration {

	@Bean
	@StepScope
	public JsonItemReader<Trade> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
		return new JsonItemReaderBuilder<Trade>().name("tradesJsonItemReader")
			.resource(resource)
			.jsonObjectReader(new GsonJsonObjectReader<>(Trade.class))
			.build();
	}

	@Bean
	@StepScope
	public JsonFileItemWriter<Trade> itemWriter(@Value("#{jobParameters[outputFile]}") WritableResource resource) {
		return new JsonFileItemWriterBuilder<Trade>().resource(resource)
			.lineSeparator("\n")
			.jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
			.name("tradesJsonFileItemWriter")
			.shouldDeleteIfExists(true)
			.build();
	}

	@Bean
	public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			JsonItemReader<Trade> itemReader, JsonFileItemWriter<Trade> itemWriter) {
		return new StepBuilder("step", jobRepository).<Trade, Trade>chunk(2)
			.transactionManager(transactionManager)
			.reader(itemReader)
			.writer(itemWriter)
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

}