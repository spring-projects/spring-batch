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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MongoJobExplorerFactoryBean;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
class MongoDBIntegrationTestConfiguration {

	@Bean
	public JobRepository jobRepository(MongoTemplate mongoTemplate, MongoTransactionManager transactionManager)
			throws Exception {
		MongoJobRepositoryFactoryBean jobRepositoryFactoryBean = new MongoJobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setMongoOperations(mongoTemplate);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.afterPropertiesSet();
		return jobRepositoryFactoryBean.getObject();
	}

	@Bean
	public JobExplorer jobExplorer(MongoTemplate mongoTemplate, MongoTransactionManager transactionManager)
			throws Exception {
		MongoJobExplorerFactoryBean jobExplorerFactoryBean = new MongoJobExplorerFactoryBean();
		jobExplorerFactoryBean.setMongoOperations(mongoTemplate);
		jobExplorerFactoryBean.setTransactionManager(transactionManager);
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}

	@Bean
	public MongoDatabaseFactory mongoDatabaseFactory(@Value("${mongo.connectionString}") String connectionString) {
		return new SimpleMongoClientDatabaseFactory(connectionString + "/test");
	}

	@Bean
	public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
		MongoTemplate template = new MongoTemplate(mongoDatabaseFactory);
		MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
		converter.setMapKeyDotReplacement(".");
		return template;
	}

	@Bean
	public MongoTransactionManager transactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
		MongoTransactionManager mongoTransactionManager = new MongoTransactionManager();
		mongoTransactionManager.setDatabaseFactory(mongoDatabaseFactory);
		mongoTransactionManager.afterPropertiesSet();
		return mongoTransactionManager;
	}

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
