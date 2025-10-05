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

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoExecutionContextDao;
import org.springframework.batch.core.repository.support.MongoExecutionContextDaoIntegrationTests.ExecutionContextDaoConfiguration;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Henning Pöttker
 * @author Yanming Zhou
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig({ MongoDBIntegrationTestConfiguration.class, ExecutionContextDaoConfiguration.class })
public class MongoExecutionContextDaoIntegrationTests {

	@BeforeAll
	static void setUp(@Autowired MongoTemplate mongoTemplate) {
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
	}

	@Test
	void testGetJobExecutionWithEmptyResult(@Autowired ExecutionContextDao executionContextDao) {
		// given
		JobInstance jobInstance = new JobInstance(1, "job");
		JobExecution jobExecution = new JobExecution(12345678L, jobInstance, new JobParameters());

		// when
		ExecutionContext actual = executionContextDao.getExecutionContext(jobExecution);

		// then
		assertNotNull(actual);
		assertTrue(actual.isEmpty());
	}

	@Test
	void testSaveJobExecution(@Autowired JobOperator jobOperator, @Autowired Job job,
			@Autowired ExecutionContextDao executionContextDao) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testSaveJobExecution")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// when
		jobExecution.getExecutionContext().putString("foo", "bar");
		executionContextDao.saveExecutionContext(jobExecution);
		ExecutionContext actual = executionContextDao.getExecutionContext(jobExecution);

		// then
		assertTrue(actual.containsKey("foo"));
		assertEquals("bar", actual.get("foo"));
	}

	@Test
	void testGetStepExecutionWithEmptyResult(@Autowired ExecutionContextDao executionContextDao) {
		// given
		JobInstance jobInstance = new JobInstance(1, "job");
		JobExecution jobExecution = new JobExecution(12345678L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(23456789L, "step", jobExecution);

		// when
		ExecutionContext actual = executionContextDao.getExecutionContext(stepExecution);

		// then
		assertNotNull(actual);
		assertTrue(actual.isEmpty());
	}

	@Test
	void testSaveStepExecution(@Autowired JobOperator jobOperator, @Autowired Job job,
			@Autowired ExecutionContextDao executionContextDao) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testSaveJobExecution")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);
		StepExecution stepExecution = jobExecution.getStepExecutions().stream().findFirst().orElseThrow();

		// when
		stepExecution.getExecutionContext().putString("foo", "bar");
		executionContextDao.saveExecutionContext(stepExecution);
		ExecutionContext actual = executionContextDao.getExecutionContext(stepExecution);

		// then
		assertTrue(actual.containsKey("foo"));
		assertEquals("bar", actual.get("foo"));
	}

	@Configuration
	static class ExecutionContextDaoConfiguration {

		@Bean
		ExecutionContextDao executionContextDao(MongoOperations mongoOperations) {
			return new MongoExecutionContextDao(mongoOperations);
		}

	}

}
