/*
 * Copyright 2009-2013 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 *
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class SimpleJobRepositoryProxyTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Advice advice;

	private JobSupport job = new JobSupport("SimpleJobRepositoryProxyTestsJob");

	@Transactional
	@Test(expected=IllegalStateException.class)
	public void testCreateAndFindWithExistingTransaction() throws Exception {
		assertFalse(advice.invoked);
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters());
		assertNotNull(jobExecution);
		assertTrue(advice.invoked);
	}

	@Test
	public void testCreateAndFindNoTransaction() throws Exception {
		assertFalse(advice.invoked);
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters());
		assertNotNull(jobExecution);
		assertTrue(advice.invoked);
	}

	public static class Advice implements MethodInterceptor {

		private boolean invoked;

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invoked = true;
			return invocation.proceed();
		}
	}

}
