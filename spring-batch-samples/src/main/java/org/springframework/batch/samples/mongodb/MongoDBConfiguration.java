/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.batch.samples.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MongoJobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MongoJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
@PropertySource("classpath:/org/springframework/batch/samples/mongodb/mongodb-sample.properties")
@EnableBatchProcessing
public class MongoDBConfiguration {

	@Value("${mongodb.host}")
	private String mongodbHost;

	@Value("${mongodb.port}")
	private int mongodbPort;

	@Value("${mongodb.database}")
	private String mongodbDatabase;

	@Bean
	public MongoClient mongoClient() {
		String connectionString = "mongodb://" + this.mongodbHost + ":" + this.mongodbPort + "/" + this.mongodbDatabase;
		return MongoClients.create(connectionString);
	}

	@Bean
	public MongoTemplate mongoTemplate(MongoClient mongoClient) {
		MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test");
		MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
		converter.setMapKeyDotReplacement(".");
		return mongoTemplate;
	}

	@Bean
	public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
		return new SimpleMongoClientDatabaseFactory(mongoClient, "test");
	}

	@Bean
	public MongoTransactionManager transactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
		return new MongoTransactionManager(mongoDatabaseFactory);
	}

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

}
