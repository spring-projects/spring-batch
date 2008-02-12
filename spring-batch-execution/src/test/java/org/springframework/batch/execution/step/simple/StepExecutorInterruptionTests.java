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

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.ItemReaderAdapter;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

public class StepExecutorInterruptionTests extends TestCase {

	private JobRepository jobRepository;

	private JobDao jobDao = new MapJobDao();

	private StepDao stepDao = new MapStepDao();

	private JobInstance job;

	private RepeatOperationsStep step;

	public void setUp() throws Exception {

		jobRepository = new SimpleJobRepository(jobDao, jobDao, stepDao, stepDao);

		JobSupport jobConfiguration = new JobSupport();
		step = new RepeatOperationsStep();
		jobConfiguration.addStep(step);
		jobConfiguration.setBeanName("testJob");
		job = jobRepository.createJobExecution(jobConfiguration, new JobParameters()).getJobInstance();
		step.setJobRepository(jobRepository);
		step.setTransactionManager(new ResourcelessTransactionManager());
		step.setItemReader(new ItemReaderAdapter());
		step.setItemWriter(new ItemWriter(){
			public void write(Object item) throws Exception {
			}});
	}

	public void testInterruptChunk() throws Exception {

		List steps = job.getStepInstances();
		final StepInstance stepInstance = (StepInstance) steps.get(0);
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(new Long(0L), new JobParameters()));
		final StepExecution stepExecution = new StepExecution(stepInstance, jobExecutionContext);
		step.setItemReader(new ItemReader() {
			public Object read() throws Exception {
				// do something non-trivial (and not Thread.sleep())
				double foo = 1;
				for (int i = 2; i < 250; i++) {
					foo = foo * i;
				}
				
				if(foo != 1){
					return new Double(foo);
				}
				else{
					return null;
				}
			}
		});

		Thread processingThread = new Thread() {
			public void run() {
				try {
					step.execute(stepExecution);
				}
				catch (JobInterruptedException e) {
					// do nothing...
				}
			}
		};

		processingThread.start();

		Thread.sleep(100);

		processingThread.interrupt();

		int count = 0;
		while (processingThread.isAlive() && count < 1000) {
			Thread.sleep(20);
			count++;
		}

		assertFalse(processingThread.isAlive());
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
	}

	public void testInterruptStep() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		// N.B, If we don't set the completion policy it might run forever
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		step.setChunkOperations(template);
		testInterruptChunk();
	}

}
