/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.validation;

import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.sample.validation.domain.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
public class ValidationSampleConfiguration {

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

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
	public Step step() throws Exception {
		return this.steps.get("step")
				.<Person, Person>chunk(1)
				.reader(itemReader())
				.processor(itemValidator())
				.writer(itemWriter())
				.build();
	}

	@Bean
	public Job job() throws Exception {
		return this.jobs.get("job")
				.start(step())
				.build();
	}

}
