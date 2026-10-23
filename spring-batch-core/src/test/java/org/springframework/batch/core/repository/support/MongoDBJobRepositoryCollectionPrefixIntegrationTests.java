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

import org.springframework.batch.core.ExitStatus;
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
import static org.springframework.batch.core.repository.dao.AbstractMongoBatchMetadataDao.DEFAULT_COLLECTION_PREFIX;
import static org.springframework.batch.core.repository.support.MongoDBCollectionPrefixIntegrationTestConfiguration.COLLECTION_PREFIX;

/**
 * Tests for a MongoDB job repository configured with a custom collection prefix.
 *
 * @author Myeongha Shin
 * @author Mahmoud Ben Hassine
 */
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBCollectionPrefixIntegrationTestConfiguration.class)
class MongoDBJobRepositoryCollectionPrefixIntegrationTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	/*
	 * The shipped schema scripts use the default collection prefix, so they are adapted
	 * here the same way users are expected to adapt them for a custom prefix.
	 */
	@BeforeEach
	void setUp() throws IOException {
		executeCommands("schema-drop-mongodb.jsonl");
		executeCommands("schema-mongodb.jsonl");
	}

	private void executeCommands(String script) throws IOException {
		List<String> commands = Files
			.lines(new FileSystemResource("src/main/resources/org/springframework/batch/core/" + script).getFilePath())
			.map(command -> command.replace(DEFAULT_COLLECTION_PREFIX, COLLECTION_PREFIX))
			.toList();
		commands.forEach(this.mongoTemplate::executeCommand);
	}

	@Test
	void testJobExecutionWithCollectionPrefix(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		// the prefixed collections hold the metadata
		assertEquals(1, countDocuments(COLLECTION_PREFIX + "JOB_INSTANCE"));
		assertEquals(1, countDocuments(COLLECTION_PREFIX + "JOB_EXECUTION"));
		assertEquals(2, countDocuments(COLLECTION_PREFIX + "STEP_EXECUTION"));

		// the default collections are left untouched
		assertEquals(0, countDocuments(DEFAULT_COLLECTION_PREFIX + "JOB_INSTANCE"));
		assertEquals(0, countDocuments(DEFAULT_COLLECTION_PREFIX + "JOB_EXECUTION"));
		assertEquals(0, countDocuments(DEFAULT_COLLECTION_PREFIX + "STEP_EXECUTION"));
	}

	@Test
	void testSequencesWithCollectionPrefix(@Autowired JobOperator jobOperator, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();

		// when
		jobOperator.start(job, jobParameters);

		// then the sequences are read from the prefixed sequences collection
		MongoCollection<Document> sequences = this.mongoTemplate.getCollection(COLLECTION_PREFIX + "SEQUENCES");
		assertEquals(1L, sequenceValue(sequences, COLLECTION_PREFIX + "JOB_INSTANCE_SEQ"));
		assertEquals(1L, sequenceValue(sequences, COLLECTION_PREFIX + "JOB_EXECUTION_SEQ"));
		assertEquals(2L, sequenceValue(sequences, COLLECTION_PREFIX + "STEP_EXECUTION_SEQ"));

		assertEquals(0, countDocuments(DEFAULT_COLLECTION_PREFIX + "SEQUENCES"));
	}

	private long countDocuments(String collectionName) {
		return this.mongoTemplate.getCollection(collectionName).countDocuments();
	}

	private long sequenceValue(MongoCollection<Document> sequences, String sequenceName) {
		Document sequence = sequences.find(new Document("_id", sequenceName)).first();
		assertNotNull(sequence, "No sequence found with id " + sequenceName);
		return sequence.getLong("count");
	}

}
