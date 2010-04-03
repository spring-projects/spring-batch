/*
 * Copyright 2006-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.test.repository;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class ConcurrentMapExecutionContextDaoTests {

	private MapExecutionContextDao dao = new MapExecutionContextDao();

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@Test
	public void testSaveUpdate() throws Exception {
		StepExecution stepExecution = new StepExecution("step", new JobExecution(11L));
		stepExecution.setId(123L);
		stepExecution.getExecutionContext().put("foo", "bar");
		dao.saveExecutionContext(stepExecution);
		ExecutionContext executionContext = dao.getExecutionContext(stepExecution);
		assertEquals("bar", executionContext.get("foo"));
	}

	@Test
	public void testTransactionalSaveUpdate() throws Exception {
		final StepExecution stepExecution = new StepExecution("step", new JobExecution(11L));
		stepExecution.setId(123L);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				stepExecution.getExecutionContext().put("foo", "bar");
				dao.saveExecutionContext(stepExecution);
				return null;
			}
		});
		ExecutionContext executionContext = dao.getExecutionContext(stepExecution);
		assertEquals("bar", executionContext.get("foo"));

	}

	@Test
	public void testConcurrentTransactionalSaveUpdate() throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(3);
		CompletionService<StepExecution> completionService = new ExecutorCompletionService<StepExecution>(executor);

		final int outerMax = 10;
		final int innerMax = 100;

		for (int i = 0; i < outerMax; i++) {

			final StepExecution stepExecution1 = new StepExecution("step", new JobExecution(11L));
			stepExecution1.setId(123L + i);
			final StepExecution stepExecution2 = new StepExecution("step", new JobExecution(11L));
			stepExecution2.setId(1234L + i);

			completionService.submit(new Callable<StepExecution>() {
				public StepExecution call() throws Exception {
					for (int i = 0; i < innerMax; i++) {
						String value = "bar" + i;
						saveAndAssert(stepExecution1, value);
					}
					return stepExecution1;
				}
			});

			completionService.submit(new Callable<StepExecution>() {
				public StepExecution call() throws Exception {
					for (int i = 0; i < innerMax; i++) {
						String value = "spam" + i;
						saveAndAssert(stepExecution2, value);
					}
					return stepExecution2;
				}
			});

			completionService.take().get();
			completionService.take().get();

		}
		
		executor.shutdown();

	}

	private void saveAndAssert(final StepExecution stepExecution, final String value) {

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				stepExecution.getExecutionContext().put("foo", value);
				dao.saveExecutionContext(stepExecution);
				return null;
			}
		});

		ExecutionContext executionContext = dao.getExecutionContext(stepExecution);
		Assert.state(executionContext != null, "Lost insert: null executionContext at value=" + value);
		String foo = executionContext.getString("foo");
		Assert.state(value.equals(foo), "Lost update: wrong value=" + value + " (found " + foo + ") for id="
				+ stepExecution.getId());

	}

}
