/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.batch.core.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class AsyncJobScopeIntegrationTests implements BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	@Qualifier("simple")
	private Collaborator simple;

	private final TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private ListableBeanFactory beanFactory;

	private int beanCount;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@BeforeEach
	void countBeans() {
		JobSynchronizationManager.release();
		beanCount = beanFactory.getBeanDefinitionCount();
	}

	@AfterEach
	void cleanUp() {
		JobSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	void testSimpleProperty() {
		JobExecution jobExecution = new JobExecution(11L);
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		JobSynchronizationManager.register(jobExecution);
		assertEquals("bar", simple.getName());
	}

	@Test
	void testGetMultipleInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<>();

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			final Long id = 123L + i;
			FutureTask<String> task = new FutureTask<>(() -> {
				JobExecution jobExecution = new JobExecution(id);
				ExecutionContext executionContext = jobExecution.getExecutionContext();
				executionContext.put("foo", value);
				JobContext context = JobSynchronizationManager.register(jobExecution);
				logger.debug("Registered: " + context.getJobExecutionContext());
				try {
					return simple.getName();
				}
				finally {
					JobSynchronizationManager.close();
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo" + i, task.get());
			i++;
		}

	}

	@Test
	void testGetSameInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<>();
		final JobExecution jobExecution = new JobExecution(11L);
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "foo");
		JobSynchronizationManager.register(jobExecution);
		assertEquals("foo", simple.getName());

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			FutureTask<String> task = new FutureTask<>(() -> {
				ExecutionContext executionContext1 = jobExecution.getExecutionContext();
				executionContext1.put("foo", value);
				JobContext context = JobSynchronizationManager.register(jobExecution);
				logger.debug("Registered: " + context.getJobExecutionContext());
				try {
					return simple.getName();
				}
				finally {
					JobSynchronizationManager.close();
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		for (FutureTask<String> task : tasks) {
			assertEquals("foo", task.get());
		}

		// Don't close the outer scope until all tasks are finished. This should
		// always be the case if using an AbstractJob
		JobSynchronizationManager.close();

	}

}
