/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import java.time.LocalDateTime;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Test for MongoDB collection prefix functionality
 *
 * @author Myeongha Shin
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBCollectionPrefixTestConfiguration.class)
public class MongoDBJobRepositoryCollectionPrefixTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	private static final String PREFIX = "TEST_COLLECTION_PREFIX_";

	@BeforeEach
	public void setUp() {
		// collections with custom prefix
		mongoTemplate.createCollection(PREFIX + "JOB_INSTANCE");
		mongoTemplate.createCollection(PREFIX + "JOB_EXECUTION");
		mongoTemplate.createCollection(PREFIX + "STEP_EXECUTION");
		// sequences
		mongoTemplate.createCollection("BATCH_SEQUENCES");
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", PREFIX + "JOB_INSTANCE_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", PREFIX + "JOB_EXECUTION_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", PREFIX + "STEP_EXECUTION_SEQ", "count", 0L)));
	}

	@Test
	void testJobExecutionWithCustomPrefix(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		// Verify data is stored in collections with custom prefix
		MongoCollection<Document> jobInstancesCollection = mongoTemplate.getCollection(PREFIX + "JOB_INSTANCE");
		MongoCollection<Document> jobExecutionsCollection = mongoTemplate.getCollection(PREFIX + "JOB_EXECUTION");
		MongoCollection<Document> stepExecutionsCollection = mongoTemplate.getCollection(PREFIX + "STEP_EXECUTION");

		Assertions.assertEquals(1, jobInstancesCollection.countDocuments());
		Assertions.assertEquals(1, jobExecutionsCollection.countDocuments());
		Assertions.assertEquals(2, stepExecutionsCollection.countDocuments());

		// Verify default collections are empty
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_JOB_INSTANCE"));
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_JOB_EXECUTION"));
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_STEP_EXECUTION"));

		System.out.println("✅ Collection prefix test passed! Data stored in: " + PREFIX + "* collections");
	}

}