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

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
public class MongoDBJobRepositoryIntegrationTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	public void setUp() throws IOException {
		ClassPathResource resource = new ClassPathResource("org/springframework/batch/core/schema-mongodb.json");
		Files.lines(resource.getFilePath()).forEach(line -> mongoTemplate.executeCommand(line));
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

	private static void dump(MongoCollection<Document> collection, String prefix) {
		for (Document document : collection.find()) {
			System.out.println(prefix + document.toJson());
		}
	}

}
