/*
 * Copyright 2020-2025 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.integration.partition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
class MessageChannelPartitionHandlerTests {

	private MessageChannelPartitionHandler messageChannelPartitionHandler;

	@Test
	void testNoPartitions() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		StepExecution managerStepExecution = mock();
		StepExecutionSplitter stepExecutionSplitter = mock();

		// execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter,
				managerStepExecution);
		// verify
		assertTrue(executions.isEmpty());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testHandleNoReply() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		StepExecution managerStepExecution = mock();
		StepExecutionSplitter stepExecutionSplitter = mock();
		MessagingTemplate operations = mock();
		Message message = mock();
		// when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(5L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step1", jobExecution);
		stepExecutions.add(stepExecution);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptySet());
		when(operations.receive((PollableChannel) any())).thenReturn(message);
		when(managerStepExecution.getJobExecution()).thenReturn(jobExecution);
		// set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		// execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter,
				managerStepExecution);
		// verify
		assertNotNull(executions);
		assertTrue(executions.isEmpty());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testHandleWithReplyChannel() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		StepExecution managerStepExecution = mock();
		StepExecutionSplitter stepExecutionSplitter = mock();
		MessagingTemplate operations = mock();
		Message message = mock();
		PollableChannel replyChannel = mock();
		// when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(5L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step1", jobExecution);
		stepExecutions.add(stepExecution);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptySet());
		when(operations.receive(replyChannel)).thenReturn(message);
		when(managerStepExecution.getJobExecution()).thenReturn(jobExecution);
		// set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setReplyChannel(replyChannel);

		// execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter,
				managerStepExecution);
		// verify
		assertNotNull(executions);
		assertTrue(executions.isEmpty());

	}

	@Test
	void messageReceiveTimeout() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		StepExecution managerStepExecution = mock();
		StepExecutionSplitter stepExecutionSplitter = mock();
		MessagingTemplate operations = mock();
		// when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(5L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step1", jobExecution);
		stepExecutions.add(stepExecution);
		when(managerStepExecution.getJobExecution()).thenReturn(jobExecution);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		// set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		// execute
		assertThrows(MessageTimeoutException.class,
				() -> messageChannelPartitionHandler.handle(stepExecutionSplitter, managerStepExecution));
	}

	@Test
	void testHandleWithJobRepositoryPolling() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		JobExecution jobExecution = new JobExecution(5L, new JobInstance(1L, "job"), new JobParameters());
		StepExecution managerStepExecution = new StepExecution(1L, "step1", jobExecution);
		StepExecutionSplitter stepExecutionSplitter = mock();
		MessagingTemplate operations = mock();
		JobRepository jobRepository = mock();
		// when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		StepExecution partition1 = new StepExecution(2L, "step1:partition1", jobExecution);
		StepExecution partition2 = new StepExecution(3L, "step1:partition2", jobExecution);
		StepExecution partition3 = new StepExecution(4L, "step1:partition3", jobExecution);
		StepExecution partition4 = new StepExecution(4L, "step1:partition3", jobExecution);
		partition1.setStatus(BatchStatus.COMPLETED);
		partition2.setStatus(BatchStatus.COMPLETED);
		partition3.setStatus(BatchStatus.STARTED);
		partition4.setStatus(BatchStatus.COMPLETED);
		stepExecutions.add(partition1);
		stepExecutions.add(partition2);
		stepExecutions.add(partition3);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		JobExecution runningJobExecution = new JobExecution(5L, new JobInstance(1L, "job"), new JobParameters());
		runningJobExecution.addStepExecutions(Arrays.asList(partition2, partition1, partition3));
		JobExecution completedJobExecution = new JobExecution(5L, new JobInstance(1L, "job"), new JobParameters());
		completedJobExecution.addStepExecutions(Arrays.asList(partition2, partition1, partition4));
		when(jobRepository.getJobExecution(5L)).thenReturn(runningJobExecution, runningJobExecution,
				runningJobExecution, completedJobExecution);

		// set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setJobRepository(jobRepository);
		messageChannelPartitionHandler.setStepName("step1");
		messageChannelPartitionHandler.setPollInterval(500L);
		messageChannelPartitionHandler.afterPropertiesSet();

		// execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter,
				managerStepExecution);
		// verify
		assertNotNull(executions);
		assertEquals(3, executions.size());
		assertTrue(executions.contains(partition1));
		assertTrue(executions.contains(partition2));
		assertTrue(executions.contains(partition4));

		// verify
		verify(operations, times(3)).send(any(Message.class));
	}

	@Test
	void testHandleWithJobRepositoryPollingTimeout() throws Exception {
		// execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		// mock
		JobExecution jobExecution = new JobExecution(5L, new JobInstance(1L, "job"), new JobParameters());
		StepExecution managerStepExecution = new StepExecution(1L, "step1", jobExecution);
		StepExecutionSplitter stepExecutionSplitter = mock();
		MessagingTemplate operations = mock();
		JobRepository jobRepository = mock();
		// when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		StepExecution partition1 = new StepExecution(2L, "step1:partition1", jobExecution);
		StepExecution partition2 = new StepExecution(3L, "step1:partition2", jobExecution);
		StepExecution partition3 = new StepExecution(4L, "step1:partition3", jobExecution);
		partition1.setStatus(BatchStatus.COMPLETED);
		partition2.setStatus(BatchStatus.COMPLETED);
		partition3.setStatus(BatchStatus.STARTED);
		stepExecutions.add(partition1);
		stepExecutions.add(partition2);
		stepExecutions.add(partition3);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		JobExecution runningJobExecution = new JobExecution(5L, new JobInstance(1L, "job"), new JobParameters());
		runningJobExecution.addStepExecutions(Arrays.asList(partition2, partition1, partition3));
		when(jobRepository.getJobExecution(5L)).thenReturn(runningJobExecution);

		// set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setJobRepository(jobRepository);
		messageChannelPartitionHandler.setStepName("step1");
		messageChannelPartitionHandler.setTimeout(1000L);
		messageChannelPartitionHandler.afterPropertiesSet();

		// execute
		assertThrows(TimeoutException.class,
				() -> messageChannelPartitionHandler.handle(stepExecutionSplitter, managerStepExecution));
	}

}
