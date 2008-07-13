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
package org.springframework.batch.integration.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.JobRepositorySupport;
import org.springframework.batch.integration.JobSupport;
import org.springframework.batch.integration.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * 
 */
public class StepExecutionMessageHandlerTests {

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.job.StepExecutionMessageHandler#setStep(org.springframework.batch.core.Step)}.
	 */
	@Test
	public void testSetStep() {
		Method method = ReflectionUtils.findMethod(StepExecutionMessageHandler.class, "setStep",
				new Class<?>[] { Step.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.job.StepExecutionMessageHandler#setJobRepository(org.springframework.batch.core.repository.JobRepository)}.
	 */
	@Test
	public void testSetJobRepository() {
		Method method = ReflectionUtils.findMethod(StepExecutionMessageHandler.class, "setJobRepository",
				new Class<?>[] { JobRepository.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testVanillaHandle() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport();
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecutionRequest message = handler.handle(new JobExecutionRequest(jobRepository.createJobExecution(
				new JobSupport("job"), new JobParameters())));
		assertEquals(1, message.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.COMPLETED, message.getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleWithInputs() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport();
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecutionRequest jobExecutionRequest = new JobExecutionRequest(jobRepository.createJobExecution(
				new JobSupport("job"), new JobParameters()));
		jobExecutionRequest.getJobExecution().getExecutionContext().putString("foo", "bar");
		JobExecutionRequest message = handler.handle(jobExecutionRequest);
		assertEquals(1, message.getJobExecution().getStepExecutions().size());
		JobExecution jobExecution = message.getJobExecution();
		assertTrue(jobExecution .getExecutionContext().containsKey("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleWithInputsAndOutputs() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport();
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecutionRequest jobExecutionRequest = new JobExecutionRequest(jobRepository.createJobExecution(
				new JobSupport("job"), new JobParameters()));
		jobExecutionRequest.getJobExecution().getExecutionContext().putString("foo", "bar");
		// The step has to add the output attribute to the context
		handler.setStep(new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.getJobExecution().getExecutionContext().putString("bar", "spam");
			}
		});
		JobExecutionRequest message = handler.handle(jobExecutionRequest);
		JobExecution jobExecution = message.getJobExecution();
		assertTrue(jobExecution .getExecutionContext().containsKey("foo"));
		assertTrue(jobExecution .getExecutionContext().containsKey("bar"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleFailedJob() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport();
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution(new JobSupport("job"), new JobParameters());
		jobExecution.setStatus(BatchStatus.FAILED);
		JobExecutionRequest message = handler.handle(new JobExecutionRequest(jobExecution));
		assertEquals(0, message.getJobExecution().getStepExecutions().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleRestart() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport() {
			@Override
			public StepExecution getLastStepExecution(JobInstance jobInstance, Step step) {
				StepExecution stepExecution = new StepExecution(step.getName(), new JobExecution(jobInstance));
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter
						.stringToProperties("foo=bar")));
				return stepExecution;
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.integration.batch.JobRepositorySupport#getStepExecutionCount(org.springframework.batch.core.JobInstance,
			 * org.springframework.batch.core.Step)
			 */
			@Override
			public int getStepExecutionCount(JobInstance jobInstance, Step step) {
				return 1;
			}
		};
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution(new JobSupport("job"), new JobParameters());
		JobExecutionRequest message = handler.handle(new JobExecutionRequest(jobExecution));
		assertNotNull(message);
		assertEquals(1, jobExecution.getStepExecutions().size());
		StepExecution stepExecution = (StepExecution) jobExecution.getStepExecutions().iterator().next();
		assertTrue(stepExecution.getExecutionContext().containsKey("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleRestartAlreadyComplete() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport() {
			@Override
			public StepExecution getLastStepExecution(JobInstance jobInstance, Step step) {
				StepExecution stepExecution = new StepExecution(step.getName(), new JobExecution(jobInstance));
				stepExecution.setStatus(BatchStatus.COMPLETED);
				stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter
						.stringToProperties("foo=bar")));
				return stepExecution;
			}
		};
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution(new JobSupport("job"), new JobParameters());
		JobExecutionRequest message = handler.handle(new JobExecutionRequest(jobExecution));
		assertNotNull(message);
		assertEquals(1, jobExecution.getStepExecutions().size());
		StepExecution stepExecution = (StepExecution) jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// We expect to get the context from the previous execution, even if we
		// do not execute
		assertTrue(stepExecution.getExecutionContext().containsKey("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleRestartStartLimitExceeded() throws Exception {
		JobRepositorySupport jobRepository = new JobRepositorySupport() {
			@Override
			public StepExecution getLastStepExecution(JobInstance jobInstance, Step step) {
				return new StepExecution(step.getName(), new JobExecution(jobInstance));
			}

			@Override
			public int getStepExecutionCount(JobInstance jobInstance, Step step) {
				// sufficiently high restart count
				return 100;
			}
		};
		StepExecutionMessageHandler handler = createHandler(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution(new JobSupport("job"), new JobParameters());
		JobExecutionRequest message = handler.handle(new JobExecutionRequest(jobExecution));
		assertNotNull(message);
		assertEquals(1, jobExecution.getStepExecutions().size());
		JobExecutionRequest payload = message;
		assertEquals(BatchStatus.FAILED, payload.getStatus());
		assertTrue(payload.hasErrors());
		Throwable error = payload.getLastThrowable();
		assertTrue(error instanceof StartLimitExceededException);
		String text = error.getMessage();
		assertTrue("Wrong exit description: " + text, text.toLowerCase().contains("start limit"));
	}

	/**
	 * @param jobRepository
	 * @return a handler for step executions
	 * 
	 */
	public StepExecutionMessageHandler createHandler(JobRepositorySupport jobRepository) {
		StepExecutionMessageHandler handler = new StepExecutionMessageHandler();
		StepSupport step = new StepSupport("step");
		step.setStartLimit(10);
		handler.setStep(step);
		handler.setJobRepository(jobRepository);
		return handler;
	}

}
