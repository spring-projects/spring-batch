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

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.integration.JobRepositorySupport;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * 
 */
public class MessageOrientedStepTests {

	private MessageOrientedStep step = new MessageOrientedStep();

	private JobExecution jobExecution;

	private DirectChannel requestChannel;

	private PollableChannel replyChannel;

	@Before
	public void createStep() {
		replyChannel = new ThreadLocalChannel();
		requestChannel = new DirectChannel();
		step.setName("step");
		step.setOutputChannel(requestChannel);
		step.setInputChannel(replyChannel);
		step.setStartLimit(10);
		step.setJobRepository(new JobRepositorySupport());
		JobInstance jobInstance = new JobInstance(0L, new JobParameters(), "job");
		jobExecution = new JobExecution(jobInstance);
	}

	@Test
	public void testSetRequestChannel() {
		Method method = ReflectionUtils.findMethod(MessageOrientedStep.class, "setOutputChannel",
				new Class<?>[] { MessageChannel.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	@Test
	public void testSetReplyChannel() {
		Method method = ReflectionUtils.findMethod(MessageOrientedStep.class, "setInputChannel",
				new Class<?>[] { PollableChannel.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.job.MessageOrientedStep#execute(org.springframework.batch.core.StepExecution)}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testExecuteWithTimeout() throws Exception {
		requestChannel.subscribe(new MessageConsumer() {
			public void onMessage(Message<?> message) {
			}
		});
		step.setExecutionTimeout(1000);
		step.setPollingInterval(100);
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		String message = stepExecution.getExitStatus().getExitDescription();
		assertTrue("Wrong message: " + message, message.contains("StepExecutionTimeoutException"));
	}

	@Test
	public void testVanillaExecute() throws Exception {
		requestChannel.subscribe(new MessageConsumer() {
			public void onMessage(Message<?> message) {
				JobExecutionRequest jobExecution = (JobExecutionRequest) message.getPayload();
				jobExecution.setStatus(BatchStatus.COMPLETED);
				replyChannel.send(message);
			}
		});
		step.execute(jobExecution.createStepExecution(step.getName()));
	}

	@Test
	public void testExecuteWithFailure() throws Exception {
		requestChannel.subscribe(new MessageConsumer() {
			public void onMessage(Message<?> message) {
				JobExecutionRequest jobExecution = (JobExecutionRequest) message.getPayload();
				jobExecution.registerThrowable(new RuntimeException("Planned failure"));
				replyChannel.send(message);
			}
		});
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		String message = stepExecution.getExitStatus().getExitDescription();
		assertTrue("Wrong message: " + message, message.contains("Planned failure"));
	}

	@Test
	public void testExecuteOnRestart() throws Exception {
		JobExecutionRequest jobExecutionRequest = new JobExecutionRequest(jobExecution);
		jobExecutionRequest.setStatus(BatchStatus.COMPLETED);
		// Send a message to the reply channel to simulate step that we were
		// waiting for when we failed on the last execution.
		replyChannel.send(new GenericMessage<JobExecutionRequest>(jobExecutionRequest));
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		stepExecution.getExecutionContext().putString(MessageOrientedStep.WAITING, "true");
		step.execute(stepExecution);
	}

}
