/*
 * Copyright 2008-2025 the original author or authors.
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

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yanming Zhou
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(MongoDBIntegrationTestConfiguration.class)
abstract class AbstractMongoDBDaoIntegrationTests {

	@Autowired
	protected JobRepository repository;

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	public void initializeSchema() throws Exception {
		// collections
		mongoTemplate.dropCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.dropCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.dropCollection("BATCH_STEP_EXECUTION");
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION");
		// sequences
		mongoTemplate.dropCollection("BATCH_SEQUENCES");
		mongoTemplate.createCollection("BATCH_SEQUENCES");
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_INSTANCE_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_JOB_EXECUTION_SEQ", "count", 0L)));
		mongoTemplate.getCollection("BATCH_SEQUENCES")
			.insertOne(new Document(Map.of("_id", "BATCH_STEP_EXECUTION_SEQ", "count", 0L)));
	}

	protected void assertTemporalEquals(LocalDateTime lhs, LocalDateTime rhs) {
		assertEquals(lhs != null ? lhs.truncatedTo(ChronoUnit.MILLIS) : lhs,
				rhs != null ? rhs.truncatedTo(ChronoUnit.MILLIS) : null);
	}

	protected void assertTemporalEquals(LocalTime lhs, LocalTime rhs) {
		assertEquals(lhs != null ? lhs.truncatedTo(ChronoUnit.MILLIS) : lhs,
				rhs != null ? rhs.truncatedTo(ChronoUnit.MILLIS) : null);
	}

}
