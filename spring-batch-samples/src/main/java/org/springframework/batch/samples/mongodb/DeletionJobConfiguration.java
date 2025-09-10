/*
 * Copyright 2020-2025 the original author or authors.
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
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.MongoPagingItemReader;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * This job will remove document "foo3" from collection "person_out" using
 * {@link MongoItemWriter#setMode(MongoItemWriter.Mode)}}.
 *
 * @author Mahmoud Ben Hassine
 */
public class DeletionJobConfiguration {

	@Bean
	public MongoPagingItemReader<Person> mongoPersonReader(MongoTemplate mongoTemplate) {
		Map<String, Sort.Direction> sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);
		return new MongoPagingItemReaderBuilder<Person>().name("personItemReader")
			.collection("person_out")
			.targetType(Person.class)
			.template(mongoTemplate)
			.query(new Query().addCriteria(where("name").is("foo3")))
			.sorts(sortOptions)
			.build();
	}

	@Bean
	public MongoItemWriter<Person> mongoPersonRemover(MongoTemplate mongoTemplate) {
		return new MongoItemWriterBuilder<Person>().template(mongoTemplate)
			.mode(MongoItemWriter.Mode.REMOVE)
			.collection("person_out")
			.build();
	}

	@Bean
	public Step deletionStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			MongoPagingItemReader<Person> mongoPersonReader, MongoItemWriter<Person> mongoPersonRemover) {
		return new StepBuilder("step", jobRepository).<Person, Person>chunk(2)
			.transactionManager(transactionManager)
			.reader(mongoPersonReader)
			.writer(mongoPersonRemover)
			.build();
	}

	@Bean
	public Job deletionJob(JobRepository jobRepository, Step deletionStep) {
		return new JobBuilder("deletionJob", jobRepository).start(deletionStep).build();
	}

}
