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

import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.dao.MongoExecutionContextDao;
import org.springframework.batch.core.repository.dao.MongoJobExecutionDao;
import org.springframework.batch.core.repository.dao.MongoJobInstanceDao;
import org.springframework.batch.core.repository.dao.MongoStepExecutionDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.Assert;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class MongoJobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean implements InitializingBean {

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
