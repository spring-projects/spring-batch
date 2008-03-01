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
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchListener;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.execution.job.SimpleJob;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobExecutionDao;
import org.springframework.batch.execution.repository.dao.MapJobInstanceDao;
import org.springframework.batch.execution.repository.dao.MapStepExecutionDao;
import org.springframework.batch.execution.step.AbstractStep;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.execution.step.support.SimpleStepFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

public class SimpleJobTests extends TestCase {

	private List recovered = new ArrayList();

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao());

	private List processed = new ArrayList();

	private ItemWriter processor = new AbstractItemWriter() {
		public void write(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private ItemReader provider;

	private SimpleJob job = new SimpleJob();;

	protected void setUp() throws Exception {
		super.setUp();
		job.setJobRepository(repository);
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
	}

	private AbstractStep getStep(String arg) throws Exception {
		return getStep(new String[] { arg });
	}

	private AbstractStep getStep(String arg0, String arg1) throws Exception {
		return getStep(new String[] { arg0, arg1 });
	}
	
	private ItemOrientedStep getStep(String[] args) throws Exception {
		SimpleStepFactoryBean factory = new SimpleStepFactoryBean();
		factory.setSingleton(false);

		List items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(args));
		provider = new ListItemReader(items);
		
		factory.setItemReader(provider);
		factory.setItemWriter(processor);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setBeanName("stepName");
		ItemOrientedStep step = (ItemOrientedStep) factory.getObject();
//		step.setItemRecoverer(new ItemRecoverer() {
//			public boolean recover(Object item, Throwable cause) {
//				recovered.add(item);
//				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
//				return true;
//			}
//		});
		return step;
	}

	public void testSimpleJob() throws Exception {

		job.setSteps(new ArrayList());
		AbstractStep step = getStep("foo", "bar");
		step.setName("step1");
		job.addStep(step);
		step = getStep("spam");
		step.setName("step2");
		job.addStep(step);

		JobInstance jobInstance = repository.createJobExecution(job, new JobParameters()).getJobInstance();

		JobExecution jobExecutionContext = new JobExecution(jobInstance);

		job.execute(jobExecutionContext);
		assertEquals(BatchStatus.COMPLETED, jobExecutionContext.getStatus());
		assertEquals(3, processed.size());
		assertTrue(processed.contains("foo"));
	}

	public void testSimpleJobWithRecovery() throws Exception {

		final List throwables = new ArrayList();

		RepeatTemplate chunkOperations = new RepeatTemplate();
		// Always handle the exception a check it is the right one...
		chunkOperations.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {
				throwables.add(throwable);
				assertEquals("Error!", throwable.getMessage());
			}
		});

		/*
		 * Each message fails once and the chunk (size=1) "rolls back"; then it
		 * is recovered ("skipped") on the second attempt (see retry policy
		 * definition above)...
		 */
		ItemOrientedStep step = getStep(new String[] { "foo", "bar", "spam" });
		
		
//		Tasklet module = getTasklet(new String[] { "foo", "bar", "spam" });
//		RepeatOperationsStep step = new RepeatOperationsStep();
		step.setChunkOperations(chunkOperations);
		step.setItemWriter(new AbstractItemWriter() {
			public void write(Object data) throws Exception {
				throw new RuntimeException("Error!");
			}
		});
		
		step.setListeners(new BatchListener[]{new ItemListenerSupport(){

			public void onReadError(Exception ex) {
				recovered.add(ex);
			}

			public void onWriteError(Exception ex, Object item) {
				recovered.add(ex);
			}
			
		}});
		
		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(0, processed.size());
		// provider should be exhausted
		assertEquals(null, provider.read());
		assertEquals(3, recovered.size());
	}

	public void testExceptionTerminates() throws Exception {
		ItemOrientedStep step = getStep(new String[] { "foo", "bar", "spam" });
		step.setName("exceptionStep");
		step.setItemWriter(new AbstractItemWriter() {
			public void write(Object data) throws Exception {
				throw new RuntimeException("Foo");
			}
		});
		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		try {
			job.execute(jobExecution);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
			// expected
		}
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
	}
	
}
