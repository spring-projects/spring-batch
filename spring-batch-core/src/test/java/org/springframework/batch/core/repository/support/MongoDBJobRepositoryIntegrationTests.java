/*
 * Copyright 2024 the original author or authors.
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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.explore.support.MongoJobExplorerFactoryBean;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Mahmoud Ben Hassine
 */
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class MongoDBJobRepositoryIntegrationTests {

	private static final DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:8.0.1");

	@Container
	public static MongoDBContainer mongodb = new MongoDBContainer(MONGODB_IMAGE);

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	public void setUp() {
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION");
		mongoTemplate.createCollection("BATCH_JOB_INSTANCE_SEQ");
		mongoTemplate.createCollection("BATCH_JOB_EXECUTION_SEQ");
		mongoTemplate.createCollection("BATCH_STEP_EXECUTION_SEQ");
		mongoTemplate.getCollection("BATCH_JOB_INSTANCE_SEQ").insertOne(new Document("count", 0));
		mongoTemplate.getCollection("BATCH_JOB_EXECUTION_SEQ").insertOne(new Document("count", 0));
		mongoTemplate.getCollection("BATCH_STEP_EXECUTION_SEQ").insertOne(new Document("count", 0));
	}

	@Test
	void testJobExecution(@Autowired JobLauncher jobLauncher, @Autowired Job job) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
			.addLocalDateTime("runtime", LocalDateTime.now())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

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

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

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
		public MongoDatabaseFactory mongoDatabaseFactory() {
			MongoClient mongoClient = MongoClients.create(mongodb.getConnectionString());
			return new SimpleMongoClientDatabaseFactory(mongoClient, "test");
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

}
