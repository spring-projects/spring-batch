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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.MongoExecutionContextDao;
import org.springframework.batch.core.repository.support.MongoExecutionContextDaoIntegrationTests.ExecutionContextDaoConfiguration;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Henning Pöttker
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig({ MongoDBIntegrationTestConfiguration.class, ExecutionContextDaoConfiguration.class })
public class MongoExecutionContextDaoIntegrationTests {

	private static final DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:8.0.1");

	@Container
	public static MongoDBContainer mongodb = new MongoDBContainer(MONGODB_IMAGE);

	@DynamicPropertySource
	static void setMongoDbConnectionString(DynamicPropertyRegistry registry) {
		registry.add("mongo.connectionString", mongodb::getConnectionString);
	}

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
		JobExecution jobExecution = new JobExecution(12345678L);

		// when
		ExecutionContext actual = executionContextDao.getExecutionContext(jobExecution);

		// then
		assertNotNull(actual);
		assertTrue(actual.isEmpty());
	}

	@Test
	void testSaveJobExecution(@Autowired JobLauncher jobLauncher, @Autowired Job job,
			@Autowired ExecutionContextDao executionContextDao) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testSaveJobExecution")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

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
		JobExecution jobExecution = new JobExecution(12345678L);
		StepExecution stepExecution = new StepExecution("step", jobExecution, 23456789L);

		// when
		ExecutionContext actual = executionContextDao.getExecutionContext(stepExecution);

		// then
		assertNotNull(actual);
		assertTrue(actual.isEmpty());
	}

	@Test
	void testSaveStepExecution(@Autowired JobLauncher jobLauncher, @Autowired Job job,
			@Autowired ExecutionContextDao executionContextDao) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "testSaveJobExecution")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);
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
