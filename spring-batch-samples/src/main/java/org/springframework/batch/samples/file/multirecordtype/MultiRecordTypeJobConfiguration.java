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
package org.springframework.batch.samples.file.multirecordtype;

import java.util.Map;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.infrastructure.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.infrastructure.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.infrastructure.item.file.transform.Range;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.Trade;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditFieldSetMapper;
import org.springframework.batch.samples.domain.trade.internal.TradeFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class MultiRecordTypeJobConfiguration {

	@Bean
	@StepScope
	public FlatFileItemReader itemReader(PatternMatchingCompositeLineMapper lineMapper,
			@Value("#{jobParameters[inputFile]}") FileSystemResource resource) {
		return new FlatFileItemReaderBuilder().name("itemReader").resource(resource).lineMapper(lineMapper).build();
	}

	@Bean
	public PatternMatchingCompositeLineMapper prefixMatchingLineMapper() {
		Map<String, FixedLengthTokenizer> tokenizers = Map.of("TRAD*", tradeLineTokenizer(), "CUST*",
				customerLineTokenizer());
		Map<String, FieldSetMapper<?>> fieldSetMappers = Map.of("TRAD*", new TradeFieldSetMapper(), "CUST*",
				new CustomerCreditFieldSetMapper());
		return new PatternMatchingCompositeLineMapper(tokenizers, fieldSetMappers);
	}

	@Bean
	public FixedLengthTokenizer tradeLineTokenizer() {
		FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
		tokenizer.setNames("isin", "quantity", "price", "customer");
		tokenizer.setColumns(new Range(5, 16), new Range(17, 19), new Range(20, 25), new Range(26, 34));
		return tokenizer;
	}

	@Bean
	public FixedLengthTokenizer customerLineTokenizer() {
		FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
		tokenizer.setNames("id", "name", "credit");
		tokenizer.setColumns(new Range(5, 9), new Range(10, 18), new Range(19, 26));
		return tokenizer;
	}

	@Bean
	@StepScope
	public FlatFileItemWriter itemWriter(DelegatingTradeLineAggregator delegatingTradeLineAggregator,
			@Value("#{jobParameters[outputFile]}") WritableResource resource) {
		return new FlatFileItemWriterBuilder().name("iemWriter")
			.resource(resource)
			.lineAggregator(delegatingTradeLineAggregator)
			.build();
	}

	@Bean
	public DelegatingTradeLineAggregator delegatingTradeLineAggregator(
			FormatterLineAggregator<Trade> tradeLineAggregator,
			FormatterLineAggregator<CustomerCredit> customerLineAggregator) {
		DelegatingTradeLineAggregator lineAggregator = new DelegatingTradeLineAggregator();
		lineAggregator.setTradeLineAggregator(tradeLineAggregator);
		lineAggregator.setCustomerLineAggregator(customerLineAggregator);
		return lineAggregator;
	}

	@Bean
	public FormatterLineAggregator<Trade> tradeLineAggregator() {
		FormatterLineAggregator<Trade> formatterLineAggregator = new FormatterLineAggregator<>("TRAD%-12s%-3d%6s%-9s");
		BeanWrapperFieldExtractor<Trade> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] { "isin", "quantity", "price", "customer" });
		formatterLineAggregator.setFieldExtractor(fieldExtractor);
		return formatterLineAggregator;
	}

	@Bean
	public FormatterLineAggregator<CustomerCredit> customerLineAggregator() {
		FormatterLineAggregator<CustomerCredit> formatterLineAggregator = new FormatterLineAggregator<>(
				"CUST%05d%-9s%08.0f");
		BeanWrapperFieldExtractor<CustomerCredit> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] { "id", "name", "credit" });
		formatterLineAggregator.setFieldExtractor(fieldExtractor);
		return formatterLineAggregator;
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			FlatFileItemReader itemReader, FlatFileItemWriter itemWriter) {
		return new JobBuilder("ioSampleJob", jobRepository)
			.start(new StepBuilder("step1", jobRepository).chunk(2)
				.transactionManager(transactionManager)
				.reader(itemReader)
				.writer(itemWriter)
				.build())
			.build();
	}

}
