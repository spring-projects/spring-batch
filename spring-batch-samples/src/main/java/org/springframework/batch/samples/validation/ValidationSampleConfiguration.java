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

package org.springframework.batch.samples.validation;

import java.util.Arrays;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.validation.domain.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class ValidationSampleConfiguration {

	@Bean
	public ListItemReader<Person> itemReader() {
		Person person1 = new Person(1, "foo");
		Person person2 = new Person(2, "");
		return new ListItemReader<>(Arrays.asList(person1, person2));
	}

	@Bean
	public ListItemWriter<Person> itemWriter() {
		return new ListItemWriter<>();
	}

	@Bean
	public BeanValidatingItemProcessor<Person> itemValidator() throws Exception {
		BeanValidatingItemProcessor<Person> validator = new BeanValidatingItemProcessor<>();
		validator.setFilter(true);
		validator.afterPropertiesSet();

		return validator;
	}

	@Bean
	public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) throws Exception {
		return new StepBuilder("step", jobRepository).<Person, Person>chunk(1)
			.transactionManager(transactionManager)
			.reader(itemReader())
			.processor(itemValidator())
			.writer(itemWriter())
			.build();
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step) throws Exception {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

}
