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
package org.springframework.batch.execution.step.simple;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.ItemReaderAdapter;
import org.springframework.batch.item.writer.ItemWriterAdapter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.interceptor.RepeatListenerAdapter;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 *
 */
public class RepeatOperationsStepTests extends TestCase {

	RepeatOperationsStep repeatStep = new RepeatOperationsStep();
	
	protected void setUp() throws Exception {
		super.setUp();
		
		repeatStep.setItemReader(new ItemReaderAdapter());
		repeatStep.setItemWriter(new ItemWriterAdapter());
	}
	
	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.RepeatOperationsStep#getChunkOperations()}.
	 */
	public void testSetChunkOperations() {
		assertNull(repeatStep.getChunkOperations());
		RepeatTemplate executor = new RepeatTemplate();
		repeatStep.setChunkOperations(executor);
		assertEquals(executor, repeatStep.getChunkOperations());
		
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.RepeatOperationsStep#getChunkOperations()}.
	 */
	public void testSetStepOperations() {
		assertNull(repeatStep.getChunkOperations());
		RepeatTemplate executor = new RepeatTemplate();
		repeatStep.setStepOperations(executor);
		assertEquals(executor, repeatStep.getStepOperations());
		
	}
	
	public void testSuccessfulRepeatOperationsHolder() throws Exception {
		RepeatTemplate repeatTemplate = new RepeatTemplate();
		final List list = new ArrayList();
		repeatTemplate.setListener(new RepeatListenerAdapter() {
			public void onError(RepeatContext context, Throwable e) {
				list.add(e);
			}
		});
		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
		RepeatOperationsStep configuration = new RepeatOperationsStep();
		configuration.setItemReader(new ItemReader(){
			public Object read() throws Exception {
				throw new NullPointerException();
			}});
		configuration.setItemWriter(new ItemWriter(){
			public void write(Object item) throws Exception {
			}});
		configuration.setChunkOperations(repeatTemplate);
		configuration.setJobRepository(new JobRepositorySupport());
		configuration.setTransactionManager(new ResourcelessTransactionManager());
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(new Long(0L), new JobParameters()),
				new Long(12)));
		try {
			configuration.execute(stepExecution);
			fail("Expected RuntimeException");
		} catch (NullPointerException e) {
			// expected
		}
		assertEquals(1, list.size());
	}

	public void testSuccessfulRepeatOperationsHolderWithStepOperations() throws Exception {
		RepeatTemplate chunkTemplate = new RepeatTemplate();
		final List list = new ArrayList();
		chunkTemplate.setListener(new RepeatListenerAdapter() {
			public void before(RepeatContext context) {
				list.add(context);
			}
		});
		chunkTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
		RepeatTemplate stepTemplate = new RepeatTemplate();
		final List steps = new ArrayList();
		stepTemplate.setListener(new RepeatListenerAdapter() {
			public void before(RepeatContext context) {
				steps.add(context);
			}
		});
		stepTemplate.setCompletionPolicy(new SimpleCompletionPolicy(1));
		RepeatOperationsStep configuration = new RepeatOperationsStep();
		configuration.setItemReader(new ItemReader(){
			public Object read() throws Exception {
				return new Object();
			}});
		configuration.setItemWriter(new ItemWriter(){
			public void write(Object item) throws Exception {
			}});
		configuration.setChunkOperations(chunkTemplate);
		configuration.setStepOperations(stepTemplate);
		configuration.setJobRepository(new JobRepositorySupport());
		configuration.setTransactionManager(new ResourcelessTransactionManager());
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(new Long(0L), new JobParameters()),
				new Long(12)));
		configuration.execute(stepExecution);
		assertEquals(2, list.size());
		assertEquals(1, steps.size());
	}
	
}
