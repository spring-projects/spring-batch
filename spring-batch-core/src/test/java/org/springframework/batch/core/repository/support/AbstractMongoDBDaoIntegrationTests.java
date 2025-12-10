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

import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.repository.dao.mongodb.MongoExecutionContextDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobInstanceDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoStepExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig({ MongoDBIntegrationTestConfiguration.class,
		AbstractMongoDBDaoIntegrationTests.MongoDBDaoConfiguration.class })
abstract class AbstractMongoDBDaoIntegrationTests {

	@BeforeEach
	void setUp(@Autowired MongoTemplate mongoTemplate) throws IOException {
		Files
			.lines(new FileSystemResource("src/main/resources/org/springframework/batch/core/schema-drop-mongodb.jsonl")
				.getFilePath())
			.forEach(mongoTemplate::executeCommand);
		Files
			.lines(new FileSystemResource("src/main/resources/org/springframework/batch/core/schema-mongodb.jsonl")
				.getFilePath())
			.forEach(mongoTemplate::executeCommand);
	}

	protected void assertTemporalEquals(LocalDateTime lhs, LocalDateTime rhs) {
		assertEquals(lhs != null ? lhs.truncatedTo(ChronoUnit.MILLIS) : lhs,
				rhs != null ? rhs.truncatedTo(ChronoUnit.MILLIS) : null);
	}

	protected void assertTemporalEquals(LocalTime lhs, LocalTime rhs) {
		assertEquals(lhs != null ? lhs.truncatedTo(ChronoUnit.MILLIS) : lhs,
				rhs != null ? rhs.truncatedTo(ChronoUnit.MILLIS) : null);
	}

	@Configuration
	static class MongoDBDaoConfiguration {

		@Bean
		MongoJobInstanceDao jobInstanceDao(MongoOperations mongoOperations) {
			return new MongoJobInstanceDao(mongoOperations);
		}

		@Bean
		MongoJobExecutionDao jobExecutionDao(MongoOperations mongoOperations, MongoJobInstanceDao jobInstanceDao) {
			MongoJobExecutionDao jobExecutionDao = new MongoJobExecutionDao(mongoOperations);
			jobExecutionDao.setJobInstanceDao(jobInstanceDao);
			return jobExecutionDao;
		}

		@Bean
		MongoStepExecutionDao stepExecutionDao(MongoOperations mongoOperations, MongoJobExecutionDao jobExecutionDao) {
			MongoStepExecutionDao stepExecutionDao = new MongoStepExecutionDao(mongoOperations);
			stepExecutionDao.setJobExecutionDao(jobExecutionDao);
			return stepExecutionDao;
		}

		@Bean
		MongoExecutionContextDao executionContextDao(MongoOperations mongoOperations) {
			return new MongoExecutionContextDao(mongoOperations);
		}

	}

}