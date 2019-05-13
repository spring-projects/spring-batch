/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.configuration.annotation;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class TransactionManagerConfigurationTests {

	@Mock
	protected static PlatformTransactionManager transactionManager;

	@Mock
	protected static PlatformTransactionManager transactionManager2;

	/*
	 * The transaction manager set on JobRepositoryFactoryBean in DefaultBatchConfigurer.createJobRepository
	 * ends up in the TransactionInterceptor advise applied to the (proxied) JobRepository.
	 * This method extracts the advise from the proxy and returns the transaction manager.
	 */
	PlatformTransactionManager getTransactionManagerSetOnJobRepository(JobRepository jobRepository) throws Exception {
		TargetSource targetSource = ((Advised) jobRepository).getTargetSource(); // proxy created in SimpleBatchConfiguration.createLazyProxy
		Advised target = (Advised) targetSource.getTarget(); // initial proxy created in AbstractJobRepositoryFactoryBean.initializeProxy
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor) {
				TransactionInterceptor transactionInterceptor = (TransactionInterceptor) advisor.getAdvice();
				return (PlatformTransactionManager) transactionInterceptor.getTransactionManager();
			}
		}
		return null;
	}

	static DataSource createDataSource() {
		return new EmbeddedDatabaseBuilder()
				.addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
				.build();
	}
}
