/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.sample.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemReaderBuilder;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 *  This job will remove document "foo3" from collection "person_out"
 *  using {@link MongoItemWriter#setDelete(boolean)}.
 *
 *  @author Mahmoud Ben Hassine
 */
@EnableBatchProcessing
public class DeletionJobConfiguration {

	private JobBuilderFactory jobBuilderFactory;
	private StepBuilderFactory stepBuilderFactory;

	public DeletionJobConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
	}

	@Bean
	public MongoItemReader<Person> mongoPersonReader(MongoTemplate mongoTemplate) {
		Map<String, Sort.Direction> sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);
		return new MongoItemReaderBuilder<Person>()
				.name("personItemReader")
				.collection("person_out")
				.targetType(Person.class)
				.template(mongoTemplate)
				.query(new Query().addCriteria(where("name").is("foo3")))
				.sorts(sortOptions)
				.build();
	}

	@Bean
	public MongoItemWriter<Person> mongoPersonRemover(MongoTemplate mongoTemplate) {
		return new MongoItemWriterBuilder<Person>()
				.template(mongoTemplate)
				.delete(true)
				.collection("person_out")
				.build();
	}

	@Bean
	public Step deletionStep(MongoItemReader<Person> mongoPersonReader, MongoItemWriter<Person> mongoPersonRemover) {
		return this.stepBuilderFactory.get("step")
				.<Person, Person>chunk(2)
				.reader(mongoPersonReader)
				.writer(mongoPersonRemover)
				.build();
	}

	@Bean
	public Job deletionJob(Step deletionStep) {
		return this.jobBuilderFactory.get("deletionJob")
				.start(deletionStep)
				.build();
	}

}
