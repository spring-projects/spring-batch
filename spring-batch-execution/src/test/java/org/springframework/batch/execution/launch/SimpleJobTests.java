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
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.job.DefaultJobExecutor;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.execution.step.SimpleStep;
import org.springframework.batch.execution.step.simple.SimpleStepExecutor;
import org.springframework.batch.execution.tasklet.ItemOrientedTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

public class SimpleJobTests extends TestCase {

	private List recovered = new ArrayList();

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobDao(), new MapStepDao());

	private List processed = new ArrayList();

	private ItemProcessor processor = new ItemProcessor() {
		public void process(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private ItemReader provider;

	private DefaultJobExecutor jobExecutor = new DefaultJobExecutor();;

	private SimpleStepExecutor stepLifecycle = new SimpleStepExecutor();

	protected void setUp() throws Exception {
		super.setUp();
		jobExecutor.setJobRepository(repository);
		stepLifecycle.setRepository(repository);
		jobExecutor.setStepExecutorFactory(new StepExecutorFactory() {
			public StepExecutor getExecutor(Step configuration) {
				return stepLifecycle;
			}
		});
	}

	private Tasklet getTasklet(String arg) throws Exception {
		return getTasklet(new String[] { arg });
	}

	private Tasklet getTasklet(String arg0, String arg1) throws Exception {
		return getTasklet(new String[] { arg0, arg1 });
	}

	private ItemOrientedTasklet getTasklet(String[] args) throws Exception {
		ItemOrientedTasklet module = new ItemOrientedTasklet();
		List items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(args));
		provider = new ListItemReader(items);
		module.setItemRecoverer(new ItemRecoverer() {
			public boolean recover(Object item, Throwable cause) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return true;
			}
		});
		module.setItemReader(provider);
		module.setItemProcessor(processor);
		module.afterPropertiesSet();
		return module;
	}

	public void testSimpleJob() throws Exception {

		Job jobConfiguration = new Job();
		JobIdentifier runtimeInformation = new ScheduledJobIdentifierFactory()
						.getJobIdentifier("real.job");

		jobConfiguration.addStep(new SimpleStep(getTasklet("foo", "bar")));
		jobConfiguration.addStep(new SimpleStep(getTasklet("spam")));

		JobInstance job = repository.findOrCreateJob(jobConfiguration, runtimeInformation).getJob();

		assertEquals(job.getName(), "real.job");

		JobExecution jobExecutionContext = new JobExecution(job);

		jobExecutor.run(jobConfiguration, jobExecutionContext);
		assertEquals(BatchStatus.COMPLETED, job.getStatus());
		assertEquals(3, processed.size());
		assertTrue(processed.contains("foo"));

	}

	public void testSimpleJobWithRecovery() throws Exception {

		Job jobConfiguration = new Job();
		JobIdentifier runtimeInformation = new SimpleJobIdentifier("real.job");
		final List throwables = new ArrayList();

		RepeatTemplate chunkOperations = new RepeatTemplate();
		// Always handle the exception a check it is the right one...
		chunkOperations.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {
				throwables.add(throwable);
				assertEquals("Try again Dummy!", throwable.getMessage());
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
		final ItemOrientedTasklet module = getTasklet(new String[] { "foo", "bar", "spam" });
		Step step = new SimpleStep(module);
		module.setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("Try again Dummy!");
			}
		});
		module.afterPropertiesSet();
		jobConfiguration.addStep(step);

		JobExecution jobExecution = repository.findOrCreateJob(jobConfiguration, runtimeInformation);
		jobExecutor.run(jobConfiguration, jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getJob().getStatus());
		assertEquals(0, processed.size());
		// provider should be exhausted
		assertEquals(null, provider.read());
		assertEquals(3, recovered.size());
	}

	public void testExceptionTerminates() throws Exception {

		Job jobConfiguration = new Job();
		JobIdentifier runtimeInformation = new SimpleJobIdentifier("real.job");
		final ItemOrientedTasklet module = getTasklet(new String[] { "foo", "bar", "spam" });
		Step step = new SimpleStep(module);
		module.setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("Foo");
			}
		});
		module.afterPropertiesSet();
		jobConfiguration.addStep(step);

		JobExecution jobExecution = repository.findOrCreateJob(jobConfiguration, runtimeInformation);
		JobInstance job = jobExecution.getJob();
		try {
			jobExecutor.run(jobConfiguration, jobExecution);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
			// expected
		}
		assertEquals(BatchStatus.FAILED, job.getStatus());
	}
}
