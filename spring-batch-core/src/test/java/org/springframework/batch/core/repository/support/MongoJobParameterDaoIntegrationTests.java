/*
 * Copyright 2008-present the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.JobParameterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * @author Moad Elfatihi
 */
class MongoJobParameterDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private JobParameterDao dao;

	private static final long DATE = 777;

	@Test
	void testDeleteJobParameterByExecution(@Autowired JobExecutionDao jobExecutionDao,
			@Autowired JobInstanceDao jobInstanceDao) {

		JobParameters jobParameters = new JobParametersBuilder().addString("stringKey", "stringValue")
			.addLong("longKey", Long.MAX_VALUE)
			.addDouble("doubleKey", Double.MAX_VALUE)
			.addDate("dateKey", new Date(DATE))
			.toJobParameters();

		JobInstance jobInstance = jobInstanceDao.createJobInstance("testParam", jobParameters);
		JobExecution jobExecution = jobExecutionDao.createJobExecution(jobInstance, jobParameters);

		dao.deleteParametersOf(jobExecution);

		JobParameters jobParametersReturned = jobExecutionDao.getJobExecution(jobExecution.getJobInstance().getId())
			.getJobParameters();

		Assert.isNull(jobParametersReturned.getString("stringKey"), "String key found");
		Assert.isNull(jobParametersReturned.getString("longKey"), "Long key found");
		Assert.isNull(jobParametersReturned.getString("doubleKey"), "Double key found");
		Assert.isNull(jobParametersReturned.getString("dateKey"), "Date key found");

	}

}