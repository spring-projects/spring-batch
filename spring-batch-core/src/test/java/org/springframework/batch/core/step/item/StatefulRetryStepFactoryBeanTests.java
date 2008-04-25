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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBeanTests extends TestCase {

	private StatefulRetryStepFactoryBean factory = new StatefulRetryStepFactoryBean();

	private List recovered = new ArrayList();

	private List processed = new ArrayList();
	
	int count = 0;	

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao());

	JobExecution jobExecution;

	private ItemWriter processor = new AbstractItemWriter() {
		public void write(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		
		factory.setBeanName("step");

		factory.setItemReader(new ListItemReader(new ArrayList()));
		factory.setItemWriter(processor);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());

		JobSupport job = new JobSupport("jobName");
		job.setRestartable(true);
		JobParameters jobParameters = new JobParametersBuilder().addString("statefulTest", "make_this_unique").toJobParameters();
		jobExecution = repository.createJobExecution(job, jobParameters);
		jobExecution.setEndTime(new Date());
		
	}

	public void testType() throws Exception {
		assertEquals(Step.class, factory.getObjectType());
	}

	public void testDefaultValue() throws Exception {
		assertTrue(factory.getObject() instanceof Step);
	}

	public void testRecovery() throws Exception {
		factory.setItemRecoverer(new ItemRecoverer() {
			public boolean recover(Object item, Throwable cause) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return true;
			}
		});
		List items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(new String[] { "a", "b", "c" }));
		ItemReader provider = new ListItemReader(items) {
			public Object read() {
				Object item = super.read();
				count++;
				if (count == 2) {
					throw new RuntimeException("Temporary error - retry for success.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		step.execute(stepExecution);
		
		assertEquals(0, stepExecution.getSkipCount());
		// b is processed twice, plus 1, plus c, plus the null at end
		assertEquals(5, count);
	}
	
	public void testSkipAndRetry() throws Exception {
		factory.setSkippableExceptionClasses(new Class[] {Exception.class});
		factory.setSkipLimit(2);
		List items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f" }));
		ItemReader provider = new ListItemReader(items) {
			public Object read() {
				Object item = super.read();
				count++;
				if ("b".equals(item) || "d".equals(item)) {
					throw new RuntimeException("Read error - planned but skippable.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		step.execute(stepExecution);
		
		assertEquals(2, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(7, count);
	}
	
	//The following test fails due to current expected behavior of retry
	//that will be addressed in 1.1
//	public void testSkipAndRetryWithWriteFailure() throws Exception {
//		
//		factory.setSkippableExceptionClasses(new Class[] {RetryException.class});
//		factory.setSkipLimit(2);
//		List items = TransactionAwareProxyFactory.createTransactionalList();
//		items.addAll(Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f" }));
//		ItemReader provider = new ListItemReader(items) {
//			public Object read() {
//				Object item = super.read();
//				System.out.print("Read Called! Item: [" + item + "]");
//				count++;				
//				return item;
//			}
//		};
//		
//		ItemWriter itemWriter = new AbstractItemWriter(){
//			public void write(Object item) throws Exception {
//				System.out.print("Write Called! Item: [" + item + "]");
//				if ("b".equals(item) || "d".equals(item)) {
//					throw new RuntimeException("Read error - planned but skippable.");
//				}			
//			}};
//		factory.setItemReader(provider);
//		factory.setItemWriter(itemWriter);
//		factory.setRetryLimit(5);
//		factory.setRetryableExceptionClasses(new Class[]{RuntimeException.class});
//		AbstractStep step = (AbstractStep) factory.getObject();
//		step.setName("mytest");
//		StepExecution stepExecution = new StepExecution(step, jobExecution);
//		step.execute(stepExecution);
//		
//		assertEquals(2, stepExecution.getSkipCount());
//		assertEquals(9, count);
//	}
}
