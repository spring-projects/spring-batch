/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.batch.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Stefano Cordio
 */
@SpringJUnitConfig
@SpringBatchTest
class SpringBatchTestIntegrationTests {

	@Autowired
	ApplicationContext context;

	@Nested
	class InnerWithoutSpringBatchTest extends BatchConfiguration {

		@Autowired
		ApplicationContext context;

		@Test
		void test() {
			assertSame(SpringBatchTestIntegrationTests.this.context, context);
			assertNotNull(context.getBean(JobOperatorTestUtils.class));
			assertNotNull(context.getBean(JobRepositoryTestUtils.class));
		}

	}

	@Nested
	@SpringBatchTest
	class InnerWithSpringBatchTest extends BatchConfiguration {

		@Autowired
		ApplicationContext context;

		@Test
		void test() {
			assertSame(SpringBatchTestIntegrationTests.this.context, context);
			assertNotNull(context.getBean(JobOperatorTestUtils.class));
			assertNotNull(context.getBean(JobRepositoryTestUtils.class));
		}

	}

	@Configuration
	static class BatchConfiguration {

		@Bean
		public JobRepository jobRepository() {
			return new ResourcelessJobRepository();
		}

		@Bean
		public JobRegistry jobRegistry() {
			return new MapJobRegistry();
		}

		@Bean
		public JobOperator jobOperator(JobRepository jobRepository, JobRegistry jobRegistry) throws Exception {
			JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
			jobOperatorFactoryBean.setJobRepository(jobRepository);
			jobOperatorFactoryBean.setJobRegistry(jobRegistry);
			jobOperatorFactoryBean.setTransactionManager(new ResourcelessTransactionManager());
			jobOperatorFactoryBean.afterPropertiesSet();
			return jobOperatorFactoryBean.getObject();
		}

	}

}
