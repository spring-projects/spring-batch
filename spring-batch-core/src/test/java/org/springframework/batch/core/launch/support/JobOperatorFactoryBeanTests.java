/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Test class for {@link JobOperatorFactoryBean}.
 *
 * @author Mahmoud Ben Hassine
 */
class JobOperatorFactoryBeanTests {

	private final PlatformTransactionManager transactionManager = Mockito.mock();

	private final JobRepository jobRepository = Mockito.mock();

	private final JobRegistry jobRegistry = Mockito.mock();

	private final JobParametersConverter jobParametersConverter = Mockito.mock();

	@Test
	public void testJobOperatorCreation() throws Exception {
		// given
		JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
		jobOperatorFactoryBean.setTransactionManager(this.transactionManager);
		jobOperatorFactoryBean.setJobRegistry(this.jobRegistry);
		jobOperatorFactoryBean.setJobRepository(this.jobRepository);
		jobOperatorFactoryBean.setJobParametersConverter(this.jobParametersConverter);
		jobOperatorFactoryBean.afterPropertiesSet();

		// when
		JobOperator jobOperator = jobOperatorFactoryBean.getObject();

		// then
		Assertions.assertNotNull(jobOperator);
		Object targetObject = AopTestUtils.getTargetObject(jobOperator);
		Assertions.assertInstanceOf(SimpleJobOperator.class, targetObject);
		Assertions.assertEquals(this.transactionManager, getTransactionManagerSetOnJobOperator(jobOperator));
	}

	@Test
	public void testCustomTransactionAttributesSource() throws Exception {
		// given
		TransactionAttributeSource transactionAttributeSource = Mockito.mock();
		JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
		jobOperatorFactoryBean.setTransactionManager(this.transactionManager);
		jobOperatorFactoryBean.setJobRegistry(this.jobRegistry);
		jobOperatorFactoryBean.setJobRepository(this.jobRepository);
		jobOperatorFactoryBean.setJobParametersConverter(this.jobParametersConverter);
		jobOperatorFactoryBean.setTransactionAttributeSource(transactionAttributeSource);
		jobOperatorFactoryBean.afterPropertiesSet();

		// when
		JobOperator jobOperator = jobOperatorFactoryBean.getObject();

		// then
		Assertions.assertEquals(transactionAttributeSource,
				getTransactionAttributesSourceSetOnJobOperator(jobOperator));

	}

	private PlatformTransactionManager getTransactionManagerSetOnJobOperator(JobOperator jobOperator) {
		Advised target = (Advised) jobOperator; // proxy created by
												// AbstractJobOperatorFactoryBean
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor transactionInterceptor) {
				return (PlatformTransactionManager) transactionInterceptor.getTransactionManager();
			}
		}
		return null;
	}

	private TransactionAttributeSource getTransactionAttributesSourceSetOnJobOperator(JobOperator jobOperator) {
		Advised target = (Advised) jobOperator; // proxy created by
		// AbstractJobOperatorFactoryBean
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor transactionInterceptor) {
				return transactionInterceptor.getTransactionAttributeSource();
			}
		}
		return null;
	}

}