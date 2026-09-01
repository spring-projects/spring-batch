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
package org.springframework.batch.core.repository.explore.support;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoExecutionContextDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobInstanceDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoStepExecutionDao;
import org.springframework.batch.core.repository.support.MongoJobRepositoryFactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.util.Assert;

/**
 * This factory bean creates a job explorer backed by MongoDB. It requires a
 * {@link MongoDatabaseFactory} or a mongo template. <strong>When providing a
 * {@link MongoOperations}, it must be configured with a {@link MappingMongoConverter}
 * having a {@code MapKeyDotReplacement} set to a non null value to support execution
 * context keys containing dots (like "step.type" or "batch.version").</strong>
 * <p>
 * For convenience, this factory can create the required {@link MongoTemplate} internally
 * when a {@link MongoDatabaseFactory} is provided.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 * @deprecated since 6.0 in favor of {@link MongoJobRepositoryFactoryBean}. Scheduled for
 * removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class MongoJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private @Nullable MongoDatabaseFactory mongoDatabaseFactory;

	private @Nullable MongoOperations mongoOperations;

	/**
	 * Set the {@link MongoDatabaseFactory} to use for creating a {@link MongoTemplate}
	 * internally. The created template will be configured with the required
	 * {@code MapKeyDotReplacement} to support execution context keys containing dots.
	 * <p>
	 * This is the recommended way to configure this factory bean, as it avoids the need
	 * to create a separate {@link MongoTemplate} specifically for Spring Batch.
	 * @param mongoDatabaseFactory the MongoDB database factory to use
	 */
	public void setMongoDatabaseFactory(MongoDatabaseFactory mongoDatabaseFactory) {
		this.mongoDatabaseFactory = mongoDatabaseFactory;
	}

	/**
	 * Set the {@link MongoOperations} to use. <strong>The provided template must be
	 * configured with a {@link MappingMongoConverter} having a
	 * {@code MapKeyDotReplacement} set to a non null value.</strong>
	 * <p>
	 * For convenience, consider using
	 * {@link #setMongoDatabaseFactory(MongoDatabaseFactory)} instead, which will create
	 * the required template internally.
	 * @param mongoOperations the MongoOperations to use
	 * @deprecated Use {@link #setMongoDatabaseFactory(MongoDatabaseFactory)} instead
	 */
	@Deprecated(since = "5.2.1", forRemoval = true)
	public void setMongoOperations(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() {
		return new MongoJobInstanceDao(this.mongoOperations);
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() {
		return new MongoJobExecutionDao(this.mongoOperations);
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() {
		return new MongoStepExecutionDao(this.mongoOperations);
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() {
		return new MongoExecutionContextDao(this.mongoOperations);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (this.mongoOperations == null && this.mongoDatabaseFactory == null) {
			throw new IllegalArgumentException(
					"Either MongoDatabaseFactory or MongoOperations must be set. Use setMongoDatabaseFactory() for automatic configuration.");
		}

		// Create MongoTemplate internally if not provided
		if (this.mongoOperations == null) {
			MongoDatabaseFactory factory = this.mongoDatabaseFactory;
			Assert.state(factory != null, "MongoDatabaseFactory must be set when MongoOperations is null");
			this.mongoOperations = createMongoTemplate(factory);
		}

		Assert.notNull(this.mongoOperations, "MongoOperations must not be null.");
	}

	/**
	 * Create a {@link MongoTemplate} configured with the required settings for Spring
	 * Batch. The template will have {@code MapKeyDotReplacement} set to support execution
	 * context keys containing dots.
	 * @param mongoDatabaseFactory the MongoDB database factory
	 * @return a configured MongoTemplate
	 */
	private MongoOperations createMongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
		MongoTemplate template = new MongoTemplate(mongoDatabaseFactory);
		MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
		// Set MapKeyDotReplacement to support keys with dots in execution context
		converter.setMapKeyDotReplacement(".");
		return template;
	}

}
