/*
 * Copyright 2025-present the original author or authors.
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

import java.util.Map;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mahmoud Ben Hassine
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobRestartIntegrationTests {

	private static final DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:8.0.1");

	@Container
	public static MongoDBContainer mongodb = new MongoDBContainer(MONGODB_IMAGE);

	@DynamicPropertySource
	static void setMongoDbConnectionString(DynamicPropertyRegistry registry) {
		registry.add("mongo.connectionString", mongodb::getConnectionString);
	}

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	JobRepository jobRepository;

	@Autowired
	MongoTransactionManager transactionManager;

	@BeforeEach
	public void setUp() {
		// collections
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION");
		// sequences
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
	void testJobExecutionRestart() throws Exception {
		// given
		Job job = new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> {
				boolean shouldFail = (boolean) chunkContext.getStepContext().getJobParameters().get("shouldfail");
				if (shouldFail) {
					throw new RuntimeException("Step failed");
				}
				return RepeatStatus.FINISHED;
			}, transactionManager).build())
			.build();
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			// shouldfail is non identifying => no effect on job instance identity
			.addJobParameter("shouldfail", true, Boolean.class, false)
			.toJobParameters();

		// First run - expected to fail
		JobExecution jobExecution1 = jobLauncher.run(job, jobParameters);
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution1.getExitStatus().getExitCode());

		// Second run - expected to fail again
		JobExecution jobExecution2 = jobLauncher.run(job, jobParameters);
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution2.getExitStatus().getExitCode());

		// Third run - expected to succeed
		jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addJobParameter("shouldfail", false, Boolean.class, false)
			.toJobParameters();
		JobExecution jobExecution3 = jobLauncher.run(job, jobParameters);
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution3.getExitStatus());

		MongoCollection<Document> jobInstancesCollection = mongoTemplate.getCollection("BATCH_JOB_INSTANCE");
		MongoCollection<Document> jobExecutionsCollection = mongoTemplate.getCollection("BATCH_JOB_EXECUTION");
		MongoCollection<Document> stepExecutionsCollection = mongoTemplate.getCollection("BATCH_STEP_EXECUTION");

		Assertions.assertEquals(1, jobInstancesCollection.countDocuments());
		Assertions.assertEquals(3, jobExecutionsCollection.countDocuments());
		Assertions.assertEquals(3, stepExecutionsCollection.countDocuments());

		JobInstance jobInstance = jobRepository.getJobInstance("job", jobParameters);
		Assertions.assertNotNull(jobInstance);
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, "step");
		Assertions.assertNotNull(lastStepExecution);
		Assertions.assertEquals(3, lastStepExecution.getId());
	}

}
