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

import java.io.IOException;
import java.nio.file.Files;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mahmoud Ben Hassine
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobRestartIntegrationTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	public void setUp() throws IOException {
		Resource resource = new FileSystemResource(
				"src/main/resources/org/springframework/batch/core/schema-mongodb.jsonl");
		Files.lines(resource.getFilePath()).forEach(line -> mongoTemplate.executeCommand(line));
	}

	@Test
	void testJobExecutionRestart(@Autowired JobOperator jobOperator, @Autowired JobRepository jobRepository,
			@Autowired MongoTransactionManager transactionManager) throws Exception {
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
		JobExecution jobExecution1 = jobOperator.start(job, jobParameters);
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution1.getExitStatus().getExitCode());

		// Second run - expected to fail again
		JobExecution jobExecution2 = jobOperator.start(job, jobParameters);
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution2.getExitStatus().getExitCode());

		// Third run - expected to succeed
		jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addJobParameter("shouldfail", false, Boolean.class, false)
			.toJobParameters();
		JobExecution jobExecution3 = jobOperator.start(job, jobParameters);
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution3.getExitStatus());

		MongoCollection<Document> jobInstancesCollection = mongoTemplate.getCollection("BATCH_JOB_INSTANCE");
		MongoCollection<Document> jobExecutionsCollection = mongoTemplate.getCollection("BATCH_JOB_EXECUTION");
		MongoCollection<Document> stepExecutionsCollection = mongoTemplate.getCollection("BATCH_STEP_EXECUTION");

		Assertions.assertEquals(1, jobInstancesCollection.countDocuments());
		Assertions.assertEquals(3, jobExecutionsCollection.countDocuments());
		Assertions.assertEquals(3, stepExecutionsCollection.countDocuments());

		JobInstance jobInstance = jobRepository.getLastJobInstance("job");
		Assertions.assertNotNull(jobInstance);
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, "step");
		Assertions.assertNotNull(lastStepExecution);
		Assertions.assertEquals(3, lastStepExecution.getId());
	}

}
