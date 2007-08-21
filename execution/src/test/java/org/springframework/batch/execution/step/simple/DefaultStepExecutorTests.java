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
import java.util.Arrays;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.runtime.JobExecutionContext;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.StepExecutionContext;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.execution.tasklet.ItemProviderProcessTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.item.provider.ListItemProvider;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

public class DefaultStepExecutorTests extends TestCase {

	ArrayList processed = new ArrayList();

	ItemProcessor processor = new ItemProcessor() {
		public void process(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private DefaultStepExecutor stepExecutor;

	private AbstractStepConfiguration stepConfiguration;

	private ItemProvider getProvider(String[] args) {
		return new ListItemProvider(Arrays.asList(args));
	}
	
	/**
	 * @param strings
	 * @return
	 */
	private Tasklet getTasklet(String[] strings) {
		ItemProviderProcessTasklet module = new ItemProviderProcessTasklet();
		module.setItemProcessor(processor);
		module.setItemProvider(getProvider(strings));
		return module;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		stepExecutor = new DefaultStepExecutor();
		stepExecutor.setRepository(new JobRepositorySupport());
		stepConfiguration = new SimpleStepConfiguration();
		stepConfiguration.setTasklet(getTasklet(new String[] {"foo", "bar", "spam"}));
		// Only process one chunk:
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setStepOperations(template);
		// Only process one item:
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);
	}

	public void testStepExecutor() throws Exception {

		StepInstance step = new StepInstance(new Long(9));
		JobExecutionContext jobExecutionContext = new JobExecutionContext(new SimpleJobIdentifier("FOO"), new JobInstance(new Long(3)));
		StepExecutionContext stepExecutionContext = new StepExecutionContext(jobExecutionContext, step);

		stepExecutor.process(stepConfiguration, stepExecutionContext);
		assertEquals(1, processed.size());
	}

	public void testChunkExecutor() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);

		StepInstance step = new StepInstance(new Long(1));
		step.setStepExecution(new StepExecution(new Long(1), new Long(2)));
		JobExecutionContext jobExecutionContext = new JobExecutionContext(new SimpleJobIdentifier("FOO"), new JobInstance(new Long(1)));

		StepExecutionContext stepExecutionContext = new StepExecutionContext(jobExecutionContext, step);
		stepExecutor.processChunk(stepConfiguration, stepExecutionContext);
		assertEquals(1, processed.size());

	}

	public void testStepContextInitialized() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);

		final StepInstance step = new StepInstance(new Long(1));
		step.setStepExecution(new StepExecution(new Long(1),new Long(1)));
		final JobExecutionContext jobExecutionContext = new JobExecutionContext(new SimpleJobIdentifier("FOO"), new JobInstance(new Long(3)));
		final StepExecutionContext stepExecutionContext = new StepExecutionContext(jobExecutionContext, step);
		
		stepConfiguration.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				assertEquals(step, stepExecutionContext.getStep());
				assertEquals(1, jobExecutionContext.getChunkContexts().size());
				assertEquals(1, jobExecutionContext.getStepContexts().size());
				assertNotNull(StepSynchronizationManager.getContext().getJobIdentifier());
				processed.add("foo");
				return ExitStatus.CONTINUABLE;
			}
		});

		stepExecutor.process(stepConfiguration, stepExecutionContext);
		assertEquals(1, processed.size());

	}

	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(new MapJobDao(), new MapStepDao());
		stepExecutor.setRepository(repository);

		StepInstance step = new StepInstance(new Long(1));
		JobExecutionContext jobExecutionContext = new JobExecutionContext(new SimpleJobIdentifier("FOO"), new JobInstance(new Long(3)));
		StepExecutionContext stepExecutionContext = new StepExecutionContext(jobExecutionContext, step);

		JobInstance job = new JobInstance(new Long(1));
		job.setIdentifier(new SimpleJobIdentifier("foo_bar"));
		
		stepExecutor.process(stepConfiguration, stepExecutionContext);
		assertEquals(1, processed.size());

		// assertEquals(1, repository.findJobs(job.?).size());
	}
	
	public void testIncrementRollbackCount(){
		
		Tasklet module = new Tasklet(){

			public ExitStatus execute() throws Exception {
				int counter = 0;
				counter++;
				
				if(counter == 1){
					throw new Exception();
				}
				
				return ExitStatus.CONTINUABLE;
			}
			
		};
		
		StepInstance step = new StepInstance(new Long(1));
		stepConfiguration.setTasklet(module);
		JobExecutionContext jobExecutionContext = new JobExecutionContext(new SimpleJobIdentifier("FOO"), new JobInstance(new Long(3)));
		StepExecutionContext stepExecutionContext = new StepExecutionContext(jobExecutionContext, step);
		
		try{
			stepExecutor.process(stepConfiguration, stepExecutionContext);
		}
		catch(Exception ex){
			assertEquals(step.getStepExecution().getRollbackCount(), new Integer(1));
		}
		
	}

}
