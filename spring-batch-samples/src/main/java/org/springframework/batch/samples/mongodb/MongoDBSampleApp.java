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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Ensure a MongoDB instance is running on "localhost:27017", otherwise modify
 * mongodb-sample.properties file as needed.
 * <p>
 * If you use docker, you can run a mongo db server with: "docker run --name mongodb --rm
 * -d -p 27017:27017 mongo"
 *
 * @author Mahmoud Ben Hassine
 */
public class MongoDBSampleApp {

	public static void main(String[] args) throws Exception {
		Class<?>[] configurationClasses = { InsertionJobConfiguration.class, DeletionJobConfiguration.class,
				MongoDBConfiguration.class };
		ApplicationContext context = new AnnotationConfigApplicationContext(configurationClasses);
		MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);

		// create meta-data collections and sequences
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION");
		mongoTemplate.createCollection("BATCH_SEQUENCES");
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_INSTANCE_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_EXECUTION_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_STEP_EXECUTION_SEQ", "count", 0L)));

		// clear collections and insert some documents in "person_in"
		MongoCollection<Document> personsIn = mongoTemplate.getCollection("person_in");
		MongoCollection<Document> personsOut = mongoTemplate.getCollection("person_out");
		personsIn.deleteMany(new Document());
		personsOut.deleteMany(new Document());
		personsIn.insertMany(Arrays.asList(new Document("name", "foo1"), new Document("name", "foo2"),
				new Document("name", "foo3"), new Document("name", "foo4")));

		// run the insertion job
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job insertionJob = context.getBean("insertionJob", Job.class);
		jobLauncher.run(insertionJob, new JobParameters());

		// check results
		List<Person> persons = mongoTemplate.findAll(Person.class, "person_out");
		System.out.println("Checking persons in person_out collection");
		for (Person person : persons) {
			System.out.println(person);
		}

		// run the deletion job
		Job deletionJob = context.getBean("deletionJob", Job.class);
		jobLauncher.run(deletionJob, new JobParameters());

		// check results (foo3 should have been removed)
		persons = mongoTemplate.findAll(Person.class, "person_out");
		System.out.println("Checking persons in person_out collection after deleting foo3");
		for (Person person : persons) {
			System.out.println(person);
		}
	}

}
