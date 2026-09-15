/*
 * Copyright 2026-present the original author or authors.
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
import java.util.List;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
class MongoDBSchemaScriptsIntegrationTests {

	private static final String CREATE_SCRIPT = "src/main/resources/org/springframework/batch/core/schema-mongodb.jsonl";

	private static final String DROP_SCRIPT = "src/main/resources/org/springframework/batch/core/schema-drop-mongodb.jsonl";

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	void setUp() throws IOException {
		executeScript(DROP_SCRIPT);
	}

	@Test
	void createScriptShouldBeIdempotent(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		executeScript(CREATE_SCRIPT);

		JobExecution firstExecution = jobOperator.start(job, jobParameters("first"));
		assertEquals(1L, firstExecution.getJobInstanceId());
		assertEquals(1L, firstExecution.getId());
		assertEquals(List.of(1L, 2L),
				firstExecution.getStepExecutions()
					.stream()
					.map(stepExecution -> stepExecution.getId())
					.sorted()
					.toList());

		executeScript(CREATE_SCRIPT);

		JobExecution secondExecution = jobOperator.start(job, jobParameters("second"));
		assertEquals(2L, secondExecution.getJobInstanceId());
		assertEquals(2L, secondExecution.getId());
		assertEquals(List.of(3L, 4L),
				secondExecution.getStepExecutions()
					.stream()
					.map(stepExecution -> stepExecution.getId())
					.sorted()
					.toList());

		assertSequenceValue("BATCH_JOB_INSTANCE_SEQ", 2L);
		assertSequenceValue("BATCH_JOB_EXECUTION_SEQ", 2L);
		assertSequenceValue("BATCH_STEP_EXECUTION_SEQ", 4L);
	}

	private JobParameters jobParameters(String name) {
		return new JobParametersBuilder().addString("name", name)
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();
	}

	private void executeScript(String path) throws IOException {
		Files.lines(new FileSystemResource(path).getFilePath()).forEach(this.mongoTemplate::executeCommand);
	}

	private void assertSequenceValue(String sequenceName, long expectedValue) {
		MongoCollection<Document> sequences = this.mongoTemplate.getCollection("BATCH_SEQUENCES");
		Document sequence = sequences.find(new Document("_id", sequenceName)).first();
		assertNotNull(sequence);
		assertEquals(expectedValue, sequence.getLong("count"));
	}

}
