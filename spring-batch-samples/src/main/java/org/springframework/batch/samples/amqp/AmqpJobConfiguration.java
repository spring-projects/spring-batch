/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.batch.samples.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.batch.item.amqp.builder.AmqpItemWriterBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Sample Configuration to demonstrate a simple reader and writer for AMQP.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class AmqpJobConfiguration {

	@Bean
	public Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("amqp-config-job", jobRepository).start(step).build();
	}

	@Bean
	public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			RabbitTemplate rabbitInputTemplate, RabbitTemplate rabbitOutputTemplate) {
		return new StepBuilder("step", jobRepository).<String, String>chunk(1, transactionManager)
			.reader(amqpItemReader(rabbitInputTemplate))
			.processor(new MessageProcessor())
			.writer(amqpItemWriter(rabbitOutputTemplate))
			.build();
	}

	/**
	 * Reads from the designated queue.
	 * @param rabbitInputTemplate the template to be used by the {@link ItemReader}.
	 * @return instance of {@link ItemReader}.
	 */
	@Bean
	public ItemReader<String> amqpItemReader(RabbitTemplate rabbitInputTemplate) {
		AmqpItemReaderBuilder<String> builder = new AmqpItemReaderBuilder<>();
		return builder.amqpTemplate(rabbitInputTemplate).build();
	}

	/**
	 * Reads from the designated destination.
	 * @param rabbitOutputTemplate the template to be used by the {@link ItemWriter}.
	 * @return instance of {@link ItemWriter}.
	 */
	@Bean
	public ItemWriter<String> amqpItemWriter(RabbitTemplate rabbitOutputTemplate) {
		AmqpItemWriterBuilder<String> builder = new AmqpItemWriterBuilder<>();
		return builder.amqpTemplate(rabbitOutputTemplate).build();
	}

}
