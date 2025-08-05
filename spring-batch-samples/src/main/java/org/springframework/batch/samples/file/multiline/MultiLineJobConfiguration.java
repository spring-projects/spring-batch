package org.springframework.batch.samples.file.multiline;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.samples.common.DataSourceConfiguration;
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
public class MultiLineJobConfiguration {

	@Bean
	@StepScope
	public MultiLineTradeItemReader itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
		FlatFileItemReader<FieldSet> delegate = new FlatFileItemReaderBuilder<FieldSet>().name("delegateItemReader")
			.resource(resource)
			.lineTokenizer(new DelimitedLineTokenizer())
			.fieldSetMapper(new PassThroughFieldSetMapper())
			.build();
		MultiLineTradeItemReader reader = new MultiLineTradeItemReader();
		reader.setDelegate(delegate);
		return reader;
	}

	@Bean
	@StepScope
	public MultiLineTradeItemWriter itemWriter(@Value("#{jobParameters[outputFile]}") WritableResource resource) {
		FlatFileItemWriter<String> delegate = new FlatFileItemWriterBuilder<String>().name("delegateItemWriter")
			.resource(resource)
			.lineAggregator(new PassThroughLineAggregator<>())
			.build();
		MultiLineTradeItemWriter writer = new MultiLineTradeItemWriter();
		writer.setDelegate(delegate);
		return writer;
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			MultiLineTradeItemReader itemReader, MultiLineTradeItemWriter itemWriter) {
		return new JobBuilder("ioSampleJob", jobRepository)
			.start(new StepBuilder("step1", jobRepository).<Trade, Trade>chunk(2, transactionManager)
				.reader(itemReader)
				.writer(itemWriter)
				.build())
			.build();
	}

}
