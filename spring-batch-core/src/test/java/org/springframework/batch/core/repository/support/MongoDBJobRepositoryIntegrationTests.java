/*
 * Copyright 2024-2025 the original author or authors.
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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author Mahmoud Ben Hassine
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobRepositoryIntegrationTests {

	private static final DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:8.0.1");

	@Container
	public static MongoDBContainer mongodb = new MongoDBContainer(MONGODB_IMAGE);

	@DynamicPropertySource
	static void setMongoDbConnectionString(DynamicPropertyRegistry registry) {
		registry.add("mongo.connectionString", mongodb::getConnectionString);
	}

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	public void setUp() {
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION");
		mongoTemplate.createCollection("BATCH_SEQUENCES");
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
				.ensureIndex(new Index().on("jobName", Sort.Direction.ASC).named("job_name_idx"));
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
				.ensureIndex(new Index().on("jobName", Sort.Direction.ASC).on("jobKey", Sort.Direction.ASC).named("job_name_key_idx"));
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
				.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.DESC).named("job_instance_idx"));
		mongoTemplate.indexOps("BATCH_JOB_EXECUTION")
				.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.ASC).named("job_instance_idx"));
		mongoTemplate.indexOps("BATCH_JOB_EXECUTION")
				.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.ASC).on("status", Sort.Direction.ASC).named("job_instance_status_idx"));
		mongoTemplate.indexOps("BATCH_STEP_EXECUTION")
				.ensureIndex(new Index().on("stepExecutionId", Sort.Direction.ASC).named("step_execution_idx"));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_INSTANCE_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_EXECUTION_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_STEP_EXECUTION_SEQ", "count", 0L)));
	}

	@Test
	void testJobExecution(@Autowired JobLauncher jobLauncher, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		// then
		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		MongoCollection<Document> jobInstancesCollection = mongoTemplate.getCollection("BATCH_JOB_INSTANCE");
		MongoCollection<Document> jobExecutionsCollection = mongoTemplate.getCollection("BATCH_JOB_EXECUTION");
		MongoCollection<Document> stepExecutionsCollection = mongoTemplate.getCollection("BATCH_STEP_EXECUTION");

		Assertions.assertEquals(1, jobInstancesCollection.countDocuments());
		Assertions.assertEquals(1, jobExecutionsCollection.countDocuments());
		Assertions.assertEquals(2, stepExecutionsCollection.countDocuments());

		// dump results for inspection
		dump(jobInstancesCollection, "job instance = ");
		dump(jobExecutionsCollection, "job execution = ");
		dump(stepExecutionsCollection, "step execution = ");
	}

	private static void dump(MongoCollection<Document> collection, String prefix) {
		for (Document document : collection.find()) {
			System.out.println(prefix + document.toJson());
		}
	}

}
