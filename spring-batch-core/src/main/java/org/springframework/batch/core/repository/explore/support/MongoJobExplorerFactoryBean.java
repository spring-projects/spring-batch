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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.util.Assert;

/**
 * This factory bean creates a job explorer backed by MongoDB. It requires a mongo
 * template and a mongo transaction manager. <strong>The mongo template must be configured
 * with a {@link MappingMongoConverter} having a {@code MapKeyDotReplacement} set to a non
 * null value. See {@code MongoDBJobRepositoryIntegrationTests} for an example. This is
 * required to support execution context keys containing dots (like "step.type" or
 * "batch.version")</strong>
 *
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 * @deprecated since 6.0 in favor of {@link MongoJobRepositoryFactoryBean}. Scheduled for
 * removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class MongoJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private MongoOperations mongoOperations;

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
		Assert.notNull(this.mongoOperations, "MongoOperations must not be null.");
	}

}
