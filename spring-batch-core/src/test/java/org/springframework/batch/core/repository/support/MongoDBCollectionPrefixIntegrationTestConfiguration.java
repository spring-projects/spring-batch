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

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableMongoJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Test configuration for a MongoDB job repository using a custom collection prefix. The
 * job repository is configured through {@link EnableMongoJobRepository} only, so that the
 * prefix is exercised end-to-end from the annotation down to the DAOs.
 *
 * @author Myeongha Shin
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableMongoJobRepository(collectionPrefix = MongoDBCollectionPrefixIntegrationTestConfiguration.COLLECTION_PREFIX)
@Import(MongoDBTestInfrastructureConfiguration.class)
class MongoDBCollectionPrefixIntegrationTestConfiguration {

	static final String COLLECTION_PREFIX = "MY_APP_";

	@Bean
	public Job job(JobRepository jobRepository, MongoTransactionManager transactionManager) {
		return new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step1", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build())
			.next(new StepBuilder("step2", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build())
			.build();
	}

}
