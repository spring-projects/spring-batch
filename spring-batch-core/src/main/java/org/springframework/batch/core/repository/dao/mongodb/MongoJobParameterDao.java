/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.batch.core.repository.dao.mongodb;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.dao.AbstractMongoBatchMetadataDao;
import org.springframework.batch.core.repository.dao.JobParameterDao;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import java.util.Collections;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Moad Elfatihi
 * @since 6.1
 */
public class MongoJobParameterDao extends AbstractMongoBatchMetadataDao implements JobParameterDao {

	private final MongoOperations mongoOperations;

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "JOB_EXECUTION";

	public MongoJobParameterDao(MongoOperations mongoOperations) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null.");
		this.mongoOperations = mongoOperations;
	}

	@Override
	public void deleteParametersOf(JobExecution jobExecution) {
		Query query = new Query(where("jobExecutionId").is(jobExecution.getId()));
		Update jobParametersRemovalUpdate = new Update().set("jobParameters", Collections.emptyList());
		this.mongoOperations.updateFirst(query, jobParametersRemovalUpdate,
				getCollectionName(JOB_EXECUTIONS_COLLECTION_NAME));
	}

}
