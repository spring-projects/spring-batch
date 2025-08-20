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
import java.util.Collections;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Mahmoud Ben Hassine
 * @author Jinwoo Bae
 * @author Yanming Zhou
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobRepositoryIntegrationTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("removal")
	@BeforeEach
	public void setUp() {
		// Clear existing collections to ensure clean state
		mongoTemplate.getCollection("BATCH_JOB_INSTANCE").drop();
		mongoTemplate.getCollection("BATCH_JOB_EXECUTION").drop();
		mongoTemplate.getCollection("BATCH_STEP_EXECUTION").drop();
		mongoTemplate.getCollection("BATCH_SEQUENCES").drop();

		// sequences
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

		// indices
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
			.ensureIndex(new Index().on("jobName", Sort.Direction.ASC).named("job_name_idx"));
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
			.ensureIndex(new Index().on("jobName", Sort.Direction.ASC)
				.on("jobKey", Sort.Direction.ASC)
				.named("job_name_key_idx"));
		mongoTemplate.indexOps("BATCH_JOB_INSTANCE")
			.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.DESC).named("job_instance_idx"));
		mongoTemplate.indexOps("BATCH_JOB_EXECUTION")
			.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.ASC).named("job_instance_idx"));
		mongoTemplate.indexOps("BATCH_JOB_EXECUTION")
			.ensureIndex(new Index().on("jobInstanceId", Sort.Direction.ASC)
				.on("status", Sort.Direction.ASC)
				.named("job_instance_status_idx"));
		mongoTemplate.indexOps("BATCH_STEP_EXECUTION")
			.ensureIndex(new Index().on("stepExecutionId", Sort.Direction.ASC).named("step_execution_idx"));
	}

	@Test
	void testJobExecution(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

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

	/**
	 * Test for GitHub issue #4943: getLastStepExecution should work when JobExecution's
	 * embedded stepExecutions array is empty.
	 *
	 * <p>
	 * This can happen after abrupt shutdown when the embedded stepExecutions array is not
	 * synchronized, but BATCH_STEP_EXECUTION collection still contains the data.
	 *
	 */
	@Test
	void testGetLastStepExecutionWithEmptyEmbeddedArray(@Autowired JobOperator jobOperator, @Autowired Job job,
			@Autowired StepExecutionDao stepExecutionDao) throws Exception {
		// Step 1: Run job normally
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "emptyArrayTest")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		JobExecution jobExecution = jobOperator.start(job, jobParameters);
		JobInstance jobInstance = jobExecution.getJobInstance();

		// Verify job completed successfully
		Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		// Step 2: Simulate the core issue - clear embedded stepExecutions array
		// while keeping BATCH_STEP_EXECUTION collection intact
		Query jobQuery = new Query(Criteria.where("jobExecutionId").is(jobExecution.getId()));
		Update jobUpdate = new Update().set("stepExecutions", Collections.emptyList());
		mongoTemplate.updateFirst(jobQuery, jobUpdate, "BATCH_JOB_EXECUTION");

		// Step 3: Verify embedded array is empty but collection still has data
		MongoCollection<Document> jobExecutionsCollection = mongoTemplate.getCollection("BATCH_JOB_EXECUTION");
		MongoCollection<Document> stepExecutionsCollection = mongoTemplate.getCollection("BATCH_STEP_EXECUTION");

		Document jobDoc = jobExecutionsCollection.find(new Document("jobExecutionId", jobExecution.getId())).first();
		Assertions.assertTrue(jobDoc.getList("stepExecutions", Document.class).isEmpty(),
				"Embedded stepExecutions array should be empty");
		Assertions.assertEquals(2, stepExecutionsCollection.countDocuments(),
				"BATCH_STEP_EXECUTION collection should still contain data");

		// Step 4: Test the fix - getLastStepExecution should work despite empty embedded
		// array
		StepExecution lastStepExecution = stepExecutionDao.getLastStepExecution(jobInstance, "step1");
		Assertions.assertNotNull(lastStepExecution,
				"getLastStepExecution should find step execution even with empty embedded array");
		Assertions.assertEquals("step1", lastStepExecution.getStepName());
		Assertions.assertEquals(BatchStatus.COMPLETED, lastStepExecution.getStatus());

		// Step 5: Test countStepExecutions also works
		long stepCount = stepExecutionDao.countStepExecutions(jobInstance, "step1");
		Assertions.assertEquals(1L, stepCount, "countStepExecutions should work despite empty embedded array");
	}

	private static void dump(MongoCollection<Document> collection, String prefix) {
		for (Document document : collection.find()) {
			System.out.println(prefix + document.toJson());
		}
	}

}
