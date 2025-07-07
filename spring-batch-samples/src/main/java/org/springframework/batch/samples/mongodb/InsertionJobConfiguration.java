/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.batch.samples.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.MongoPagingItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This job will copy documents from collection "person_in" into collection "person_out"
 * using {@link MongoPagingItemReader} and {@link MongoItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 */
public class InsertionJobConfiguration {

	@Bean
	public MongoPagingItemReader<Person> mongoItemReader(MongoTemplate mongoTemplate) {
		Map<String, Sort.Direction> sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);
		return new MongoPagingItemReaderBuilder<Person>().name("personItemReader")
			.collection("person_in")
			.targetType(Person.class)
			.template(mongoTemplate)
			.jsonQuery("{}")
			.sorts(sortOptions)
			.build();
	}

	@Bean
	public MongoItemWriter<Person> mongoItemWriter(MongoTemplate mongoTemplate) {
		return new MongoItemWriterBuilder<Person>().template(mongoTemplate).collection("person_out").build();
	}

	@Bean
	public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			MongoPagingItemReader<Person> mongoItemReader, MongoItemWriter<Person> mongoItemWriter) {
		return new StepBuilder("step", jobRepository).<Person, Person>chunk(2, transactionManager)
			.reader(mongoItemReader)
			.writer(mongoItemWriter)
			.build();
	}

	@Bean
	public Job insertionJob(JobRepository jobRepository, Step step) {
		return new JobBuilder("insertionJob", jobRepository).start(step).build();
	}

}
