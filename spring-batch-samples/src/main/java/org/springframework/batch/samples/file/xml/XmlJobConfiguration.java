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
package org.springframework.batch.samples.file.xml;

import java.math.BigDecimal;
import java.util.Map;

import com.thoughtworks.xstream.security.ExplicitTypePermission;

import org.springframework.batch.core.job.Job;
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
import org.springframework.oxm.xstream.XStreamMarshaller;

@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
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
			.start(new StepBuilder("step1", jobRepository).<CustomerCredit, CustomerCredit>chunk(2)
				.transactionManager(transactionManager)
				.reader(itemReader)
				.processor(new CustomerCreditIncreaseProcessor())
				.writer(itemWriter)
				.build())
			.build();
	}

}
