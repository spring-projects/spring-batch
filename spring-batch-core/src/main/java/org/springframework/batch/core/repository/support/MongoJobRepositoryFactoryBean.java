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

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.repository.dao.mongodb.MongoExecutionContextDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobInstanceDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoSequenceIncrementer;
import org.springframework.batch.core.repository.dao.mongodb.MongoStepExecutionDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * This factory bean creates a job repository backed by MongoDB. It requires a mongo
 * template and a mongo transaction manager. <strong>The mongo template must be configured
 * with a {@link MappingMongoConverter} having a {@code MapKeyDotReplacement} set to a non
 * null value. See {@code MongoDBJobRepositoryIntegrationTests} for an example. This is
 * required to support execution context keys containing dots (like "step.type" or
 * "batch.version")</strong>
 *
 * @author Mahmoud Ben Hassine
 * @author Myeongha Shin
 * @since 5.2.0
 */
public class MongoJobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean implements InitializingBean {

	private @Nullable MongoOperations mongoOperations;

	private @Nullable DataFieldMaxValueIncrementer jobInstanceIncrementer;

	private @Nullable DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private @Nullable DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private @Nullable String collectionPrefix = "BATCH_";

	public void setMongoOperations(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	public void setJobInstanceIncrementer(DataFieldMaxValueIncrementer jobInstanceIncrementer) {
		this.jobInstanceIncrementer = jobInstanceIncrementer;
	}

	public void setJobExecutionIncrementer(DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	public void setStepExecutionIncrementer(DataFieldMaxValueIncrementer stepExecutionIncrementer) {
		this.stepExecutionIncrementer = stepExecutionIncrementer;
	}

	public void setCollectionPrefix(String collectionPrefix) {
		this.collectionPrefix = collectionPrefix;
	}

	@Override
	protected Object getTarget() throws Exception {
		MongoJobInstanceDao jobInstanceDao = createJobInstanceDao();
		MongoJobExecutionDao jobExecutionDao = createJobExecutionDao();
		jobExecutionDao.setJobInstanceDao(jobInstanceDao);
		MongoStepExecutionDao stepExecutionDao = createStepExecutionDao();
		stepExecutionDao.setJobExecutionDao(jobExecutionDao);
		MongoExecutionContextDao executionContextDao = createExecutionContextDao();
		return new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao, executionContextDao);
	}

	@Override
	protected MongoJobInstanceDao createJobInstanceDao() {
		MongoJobInstanceDao mongoJobInstanceDao = new MongoJobInstanceDao(this.mongoOperations, this.collectionPrefix);
		mongoJobInstanceDao.setJobKeyGenerator(this.jobKeyGenerator);
		mongoJobInstanceDao.setJobInstanceIncrementer(this.jobInstanceIncrementer);
		return mongoJobInstanceDao;
	}

	@Override
	protected MongoJobExecutionDao createJobExecutionDao() {
		MongoJobExecutionDao mongoJobExecutionDao = new MongoJobExecutionDao(this.mongoOperations,
				this.collectionPrefix);
		mongoJobExecutionDao.setJobExecutionIncrementer(this.jobExecutionIncrementer);
		return mongoJobExecutionDao;
	}

	@Override
	protected MongoStepExecutionDao createStepExecutionDao() {
		MongoStepExecutionDao mongoStepExecutionDao = new MongoStepExecutionDao(this.mongoOperations,
				this.collectionPrefix);
		mongoStepExecutionDao.setStepExecutionIncrementer(this.stepExecutionIncrementer);
		return mongoStepExecutionDao;
	}

	@Override
	protected MongoExecutionContextDao createExecutionContextDao() {
		return new MongoExecutionContextDao(this.mongoOperations, this.collectionPrefix);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(this.mongoOperations, "MongoOperations must not be null.");
		
		if (this.jobInstanceIncrementer == null) {
			this.jobInstanceIncrementer = new MongoSequenceIncrementer(this.mongoOperations, "JOB_INSTANCE_SEQ",
					this.collectionPrefix);
		}
		if (this.jobExecutionIncrementer == null) {
			this.jobExecutionIncrementer = new MongoSequenceIncrementer(this.mongoOperations, "JOB_EXECUTION_SEQ",
					this.collectionPrefix);
		}
		if (this.stepExecutionIncrementer == null) {
			this.stepExecutionIncrementer = new MongoSequenceIncrementer(this.mongoOperations, "STEP_EXECUTION_SEQ",
					this.collectionPrefix);
		}
	}

}
