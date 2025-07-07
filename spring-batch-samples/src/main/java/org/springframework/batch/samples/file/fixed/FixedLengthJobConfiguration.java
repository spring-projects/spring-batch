package org.springframework.batch.samples.file.fixed;

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
import org.springframework.batch.item.file.transform.Range;
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
@Import(DataSourceConfiguration.class)
public class FixedLengthJobConfiguration {

	@Bean
	@StepScope
	public FlatFileItemReader<CustomerCredit> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
		return new FlatFileItemReaderBuilder<CustomerCredit>().name("itemReader")
			.resource(resource)
			.fixedLength()
			.columns(new Range(1, 9), new Range(10, 11))
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
			.formatted()
			.format("%-9s%-2.0f")
			.names("name", "credit")
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			ItemReader<CustomerCredit> itemReader, ItemWriter<CustomerCredit> itemWriter) {
		return new JobBuilder("ioSampleJob", jobRepository)
			.start(new StepBuilder("step1", jobRepository).<CustomerCredit, CustomerCredit>chunk(2, transactionManager)
				.reader(itemReader)
				.processor(new CustomerCreditIncreaseProcessor())
				.writer(itemWriter)
				.build())
			.build();
	}

}
