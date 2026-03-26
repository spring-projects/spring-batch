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
package org.springframework.batch.core.configuration.support;

import java.util.Map;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MongoDBTestInfrastructureConfiguration;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Myeongha Shin
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDefaultBatchConfigurationTests.MyJobConfiguration.class)
class MongoDefaultBatchConfigurationTests {

	private static final String COLLECTION_PREFIX = "TEST_COLLECTION_PREFIX_";

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	void setUp() {
		createCollections(this.mongoTemplate);
	}

	@Test
	void testCustomCollectionPrefix(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertEquals(1, mongoTemplate.getCollection(COLLECTION_PREFIX + "JOB_INSTANCE").countDocuments());
		Assertions.assertEquals(1, mongoTemplate.getCollection(COLLECTION_PREFIX + "JOB_EXECUTION").countDocuments());
		Assertions.assertEquals(1, mongoTemplate.getCollection(COLLECTION_PREFIX + "STEP_EXECUTION").countDocuments());
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_JOB_INSTANCE"));
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_JOB_EXECUTION"));
		Assertions.assertFalse(mongoTemplate.collectionExists("BATCH_STEP_EXECUTION"));
	}

	private void createCollections(MongoTemplate mongoTemplate) {
		mongoTemplate.createCollection(COLLECTION_PREFIX + "JOB_INSTANCE");
		mongoTemplate.createCollection(COLLECTION_PREFIX + "JOB_EXECUTION");
		mongoTemplate.createCollection(COLLECTION_PREFIX + "STEP_EXECUTION");
		mongoTemplate.createCollection(COLLECTION_PREFIX + "SEQUENCES");
		MongoCollection<Document> sequencesCollection = mongoTemplate.getCollection(COLLECTION_PREFIX + "SEQUENCES");
		sequencesCollection.insertOne(new Document(Map.of("_id", COLLECTION_PREFIX + "JOB_INSTANCE_SEQ", "count", 0L)));
		sequencesCollection
			.insertOne(new Document(Map.of("_id", COLLECTION_PREFIX + "JOB_EXECUTION_SEQ", "count", 0L)));
		sequencesCollection
			.insertOne(new Document(Map.of("_id", COLLECTION_PREFIX + "STEP_EXECUTION_SEQ", "count", 0L)));
	}

	@Configuration
	@Import(MongoDBTestInfrastructureConfiguration.class)
	static class MyJobConfiguration extends MongoDefaultBatchConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, MongoTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step1", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
					.build())
				.build();
		}

		@Override
		protected String getCollectionPrefix() {
			return COLLECTION_PREFIX;
		}

	}

}
