package org.springframework.batch.sample.file.xml;

import java.math.BigDecimal;
import java.util.Map;

import javax.sql.DataSource;

import com.thoughtworks.xstream.security.ExplicitTypePermission;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.oxm.xstream.XStreamMarshaller;

@Configuration
@EnableBatchProcessing
public class XmlJobConfiguration {

	@Bean
	public XStreamMarshaller customerCreditMarshaller() {
		XStreamMarshaller marshaller = new XStreamMarshaller();
		marshaller
			.setAliases(Map.of("customer", CustomerCredit.class, "credit", BigDecimal.class, "name", String.class));
		marshaller.setTypePermissions(new ExplicitTypePermission(new Class[] { CustomerCredit.class }));
		return marshaller;
	}

	@Bean
	@StepScope
	public StaxEventItemReader<CustomerCredit> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
		return new StaxEventItemReaderBuilder<CustomerCredit>().name("itemReader")
			.resource(resource)
			.addFragmentRootElements("customer")
			.unmarshaller(customerCreditMarshaller())
			.build();
	}

	@Bean
	@StepScope
	public StaxEventItemWriter<CustomerCredit> itemWriter(
			@Value("#{jobParameters[outputFile]}") WritableResource resource) {
		return new StaxEventItemWriterBuilder<CustomerCredit>().name("itemWriter")
			.resource(resource)
			.marshaller(customerCreditMarshaller())
			.rootTagName("customers")
			.overwriteOutput(true)
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

	// Infrastructure beans

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.generateUniqueName(true)
			.build();
	}

	@Bean
	public JdbcTransactionManager transactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

}
