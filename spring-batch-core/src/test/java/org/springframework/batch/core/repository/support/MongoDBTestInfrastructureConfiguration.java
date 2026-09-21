/*
 * Copyright 2026 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MongoDB test infrastructure.
 *
 * @author Myeongha Shin
 */
@Configuration
public class MongoDBTestInfrastructureConfiguration {

	private static final DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:8.0.11");

	@Bean(initMethod = "start")
	public MongoDBContainer mongoDBContainer() {
		return new MongoDBContainer(MONGODB_IMAGE).withReplicaSet();
	}

	@Bean
	public MongoDatabaseFactory mongoDatabaseFactory(MongoDBContainer mongoDBContainer) {
		return new SimpleMongoClientDatabaseFactory(mongoDBContainer.getConnectionString() + "/test");
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

}
