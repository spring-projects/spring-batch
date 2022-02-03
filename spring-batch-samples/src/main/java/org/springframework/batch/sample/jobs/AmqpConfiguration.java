/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.batch.sample.jobs;

import javax.sql.DataSource;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.batch.item.amqp.builder.AmqpItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Sample Configuration to demonstrate a simple reader and writer for AMQP.
 *
 * @author Glenn Renfro
 */
@Configuration
@EnableBatchProcessing
public class AmqpConfiguration {

	public final static String QUEUE_NAME = "rabbitmq.test.queue";

	public final static String EXCHANGE_NAME = "rabbitmq.test.exchange";

	private final static int amqpPort = 5672;

	private final static String host = "127.0.0.1";

	@Autowired
	JobRepository jobRepository;

	@Autowired
	StepBuilderFactory stepBuilderFactory;

	@Bean
	public Job job(JobBuilderFactory jobs, Step step) {
		return jobs.get("amqp-config-job")
				.start(step)
				.build();
	}

	@Bean
	public Step step(RabbitTemplate rabbitInputTemplate, RabbitTemplate rabbitOutputTemplate) {
		return stepBuilderFactory.get("step")
				.<String, String>chunk(1)
				.reader(amqpItemReader(rabbitInputTemplate))
				.writer(amqpItemWriter(rabbitOutputTemplate))
				.build();
	}

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.addScript("/business-schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
	}

	/**
	 * Reads from the designated queue.
	 * @param template the template to be used by the {@link ItemReader}.
	 * @return instance of {@link ItemReader}.
	 */
	@Bean
	public ItemReader amqpItemReader(RabbitTemplate template) {
		AmqpItemReaderBuilder builder = new AmqpItemReaderBuilder();
		return builder.amqpTemplate(template).build();
	}

	/**
	 * Reads from the designated destination.
	 * @param template the template to be used by the {@link ItemWriter}.
	 * @return instance of {@link ItemWriter}.
	 */
	@Bean
	public ItemWriter amqpItemWriter(RabbitTemplate template) {
		AmqpItemWriterBuilder builder = new AmqpItemWriterBuilder();
		return builder.amqpTemplate(template).build();
	}

	/**
	 * @return {@link CachingConnectionFactory} to be used by the {@link AmqpTemplate}
	 */
	@Bean
	public CachingConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(host, amqpPort);
	}
	/**
	 * @return {@link AmqpTemplate} to be used for the {@link ItemWriter}
	 */
	@Bean
	public AmqpTemplate rabbitOutputTemplate(CachingConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(new Jackson2JsonMessageConverter());
		template.setExchange(EXCHANGE_NAME);
		return template;
	}

	/**
	 * @return {@link AmqpTemplate} to be used for the {@link ItemReader}.
	 */
	@Bean
	public RabbitTemplate rabbitInputTemplate() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, amqpPort);
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(new Jackson2JsonMessageConverter());
		template.setDefaultReceiveQueue(QUEUE_NAME);
		return template;
	}
}
