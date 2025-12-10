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

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Henning Pöttker
 * @author Yanming Zhou
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobExplorerIntegrationTests {

	@Autowired
	private JobRepository jobRepository;

	@BeforeAll
	static void setUp(@Autowired MongoTemplate mongoTemplate) throws IOException {
		Files
			.lines(new FileSystemResource("src/main/resources/org/springframework/batch/core/schema-drop-mongodb.jsonl")
				.getFilePath())
			.forEach(mongoTemplate::executeCommand);
		Files
			.lines(new FileSystemResource("src/main/resources/org/springframework/batch/core/schema-mongodb.jsonl")
				.getFilePath())
			.forEach(mongoTemplate::executeCommand);
	}

	@Test
	void testGetJobExecutionById(@Autowired JobOperator jobOperator, @Autowired Job job,
			@Autowired JobRepository jobRepository) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testGetJobExecutionById")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// when
		JobExecution actual = jobRepository.getJobExecution(jobExecution.getId());

		// then
		assertNotNull(actual);
		assertNotNull(actual.getJobInstance());
		assertEquals(jobExecution.getJobInstanceId(), actual.getJobInstanceId());
		assertFalse(actual.getExecutionContext().isEmpty());
	}

	@Test
	void testGetStepExecutionByIds(@Autowired JobOperator jobOperator, @Autowired Job job,
			@Autowired JobRepository jobRepository) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testGetStepExecutionByIds")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);
		StepExecution stepExecution = jobExecution.getStepExecutions().stream().findFirst().orElseThrow();

		// when
		StepExecution actual = jobRepository.getStepExecution(jobExecution.getId(), stepExecution.getId());

		// then
		assertNotNull(actual);
		assertEquals(stepExecution.getId(), actual.getId());
		assertFalse(actual.getExecutionContext().isEmpty());
	}

}
