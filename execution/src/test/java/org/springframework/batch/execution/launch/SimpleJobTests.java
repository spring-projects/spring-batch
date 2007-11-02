/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.execution.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.job.DefaultJobExecutor;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.execution.step.SimpleStepConfiguration;
import org.springframework.batch.execution.step.simple.DefaultStepExecutor;
import org.springframework.batch.execution.tasklet.ItemProviderProcessTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.item.provider.ListItemProvider;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

public class SimpleJobTests extends TestCase {

	private List list = new ArrayList();

	// private int count;

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobDao(), new MapStepDao());

	private List processed = new ArrayList();

	private ItemProcessor processor = new ItemProcessor() {
		public void process(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private ItemProvider provider;

	private DefaultJobExecutor jobLifecycle = new DefaultJobExecutor();;

	private DefaultStepExecutor stepLifecycle = new DefaultStepExecutor();

	protected void setUp() throws Exception {
		super.setUp();
		jobLifecycle.setJobRepository(repository);
		stepLifecycle.setRepository(repository);
		jobLifecycle.setStepExecutorFactory(new StepExecutorFactory() {
			public StepExecutor getExecutor(StepConfiguration configuration) {
				return stepLifecycle;
			}
		});
	}

	private Tasklet getTasklet(String arg) {
		return getTasklet(new String[] { arg });
	}

	private Tasklet getTasklet(String arg0, String arg1) {
		return getTasklet(new String[] { arg0, arg1 });
	}

	private Tasklet getTasklet(String[] args) {
		ItemProviderProcessTasklet module = new ItemProviderProcessTasklet();
		List items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(args));
		provider = new ListItemProvider(items) {
			public boolean recover(Object item, Throwable cause) {
				list.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return true;
			}
		};
		module.setItemProvider(provider);
		module.setItemProcessor(processor);
		return module;
	}

	public void testSimpleJob() throws Exception {

		JobConfiguration jobConfiguration = new JobConfiguration();
		JobIdentifier runtimeInformation = new ScheduledJobIdentifierFactory()
						.getJobIdentifier("real.job");

		jobConfiguration.addStep(new SimpleStepConfiguration(getTasklet("foo", "bar")));
		jobConfiguration.addStep(new SimpleStepConfiguration(getTasklet("spam")));

		JobInstance job = repository.findOrCreateJob(jobConfiguration, runtimeInformation);

		assertEquals(job.getName(), "real.job");

		JobExecution jobExecutionContext = new JobExecution(job);

		jobLifecycle.run(jobConfiguration, jobExecutionContext);
		assertEquals(BatchStatus.COMPLETED, job.getStatus());
		assertEquals(3, processed.size());
		assertTrue(processed.contains("foo"));

	}

	public void testSimpleJobWithRecovery() throws Exception {

		JobConfiguration jobConfiguration = new JobConfiguration();
		JobIdentifier runtimeInformation = new SimpleJobIdentifier("real.job");

		RepeatTemplate chunkOperations = new RepeatTemplate();
		// Always handle the exception a check it is the right one...
		chunkOperations.setExceptionHandler(new ExceptionHandler() {
			public void handleExceptions(RepeatContext context, Collection throwables) {
				assertEquals(1, throwables.size());
				assertEquals("Try again Dummy!", ((Throwable) throwables.iterator().next()).getMessage());
			}
		});
		stepLifecycle.setChunkOperations(chunkOperations);

		TransactionProxyFactoryBean proxyFactoryBean = new TransactionProxyFactoryBean();
		proxyFactoryBean.setTransactionManager(new ResourcelessTransactionManager());
		proxyFactoryBean.setTarget(stepLifecycle);
		proxyFactoryBean.setTransactionAttributes(StringUtils.splitArrayElementsIntoProperties(
				new String[] { "processChunk=PROPAGATION_REQUIRED" }, "="));
		proxyFactoryBean.setExposeProxy(true);
		proxyFactoryBean.afterPropertiesSet();

		/*
		 * Each message fails once and the chunk (size=1) "rolls back"; then it
		 * is recovered ("skipped") on the second attempt (see retry policy
		 * definition above)...
		 */
		final Tasklet module = getTasklet(new String[] { "foo", "bar", "spam" });
		StepConfiguration step = new SimpleStepConfiguration(module);
		((ItemProviderProcessTasklet) module).setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("Try again Dummy!");
			}
		});
		jobConfiguration.addStep(step);

		JobInstance job = repository.findOrCreateJob(jobConfiguration, runtimeInformation);
		JobExecution jobExecutionContext = new JobExecution(job);
		jobLifecycle.run(jobConfiguration, jobExecutionContext);

		assertEquals(BatchStatus.COMPLETED, job.getStatus());
		assertEquals(0, processed.size());
		// provider should be exhausted
		assertEquals(null, provider.next());
		assertEquals(3, list.size());
	}

	public void testExceptionTerminates() throws Exception {

		JobConfiguration jobConfiguration = new JobConfiguration();
		JobIdentifier runtimeInformation = new SimpleJobIdentifier("real.job");
		final Tasklet module = getTasklet(new String[] { "foo", "bar", "spam" });
		StepConfiguration step = new SimpleStepConfiguration(module);
		((ItemProviderProcessTasklet) module).setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("Foo");
			}
		});
		jobConfiguration.addStep(step);

		JobInstance job = repository.findOrCreateJob(jobConfiguration, runtimeInformation);
		JobExecution jobExecutionContext = new JobExecution(job);
		try {
			jobLifecycle.run(jobConfiguration, jobExecutionContext);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
			// expected
		}
		assertEquals(BatchStatus.FAILED, job.getStatus());
	}
}
