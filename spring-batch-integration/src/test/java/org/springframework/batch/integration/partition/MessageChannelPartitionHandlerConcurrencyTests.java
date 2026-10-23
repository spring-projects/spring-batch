/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.partition;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for
 * <a href="https://github.com/spring-projects/spring-batch/issues/4133">issue 4133</a>
 * and <a href="https://github.com/spring-projects/spring-batch/issues/4945">issue
 * 4945</a>: two concurrent job executions sharing the same
 * {@link MessageChannelPartitionHandler} (as happens when the manager step bean is a
 * singleton, the default unless explicitly job/step scoped) must never receive each
 * other's aggregated worker replies, regardless of which job's partitions finish first.
 *
 * @author Mahmoud Ben Hassine
 */
class MessageChannelPartitionHandlerConcurrencyTests {

	@Test
	void concurrentJobExecutionsSharingTheSameHandlerDoNotCrossReplies() throws Exception {
		MessageChannelPartitionHandler handler = new MessageChannelPartitionHandler();
		handler.setStepName("workerStep");

		// Mirrors the wiring built by RemotePartitioningManagerStepBuilder: an aggregator
		// correlating worker replies by correlation id (job execution id : step name),
		// with its output going to the handler's (now subscribable) reply channel.
		DirectChannel replyChannel = new DirectChannel();
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(
				new DefaultAggregatingMessageGroupProcessor(), new SimpleMessageStore());
		aggregator.setOutputChannel(replyChannel);
		DirectChannel repliesFromWorkers = new DirectChannel();
		repliesFromWorkers.subscribe(aggregator);

		CountDownLatch job1RequestsSent = new CountDownLatch(2);
		CountDownLatch job2RequestsSent = new CountDownLatch(2);
		MessagingTemplate operations = mock();
		when(operations.getReceiveTimeout()).thenReturn(-1L);
		doAnswer(invocation -> {
			Message<?> request = invocation.getArgument(0);
			String correlationId = (String) request.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID);
			if (correlationId.startsWith("1:")) {
				job1RequestsSent.countDown();
			}
			else {
				job2RequestsSent.countDown();
			}
			return null;
		}).when(operations).send(any());

		handler.setMessagingOperations(operations);
		handler.setReplyChannel(replyChannel);
		handler.afterPropertiesSet();

		StepExecutionSplitter splitter1 = fixedSplitter(1L, Set.of(2L, 3L));
		StepExecutionSplitter splitter2 = fixedSplitter(2L, Set.of(4L, 5L));
		StepExecution manager1 = new StepExecution(10L, "managerStep",
				new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters()));
		StepExecution manager2 = new StepExecution(11L, "managerStep",
				new JobExecution(2L, new JobInstance(2L, "job"), new JobParameters()));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			@SuppressWarnings("unchecked")
			Future<Set<StepExecution>> job1Result = executor
				.submit(() -> (Set<StepExecution>) handler.handle(splitter1, manager1));
			@SuppressWarnings("unchecked")
			Future<Set<StepExecution>> job2Result = executor
				.submit(() -> (Set<StepExecution>) handler.handle(splitter2, manager2));

			// Wait until both manager threads have registered their correlation id and
			// sent their requests, so that replying can never race ahead of registration.
			assertTrue(job1RequestsSent.await(5, TimeUnit.SECONDS));
			assertTrue(job2RequestsSent.await(5, TimeUnit.SECONDS));

			// Job 2's partitions finish and reply first, even though job 1 sent its
			// requests first - this is exactly the ordering that used to cross replies.
			repliesFromWorkers.send(workerReply(2L, 4L, 0, 2));
			repliesFromWorkers.send(workerReply(2L, 5L, 1, 2));
			repliesFromWorkers.send(workerReply(1L, 2L, 0, 2));
			repliesFromWorkers.send(workerReply(1L, 3L, 1, 2));

			Set<StepExecution> job1StepExecutions = job1Result.get(5, TimeUnit.SECONDS);
			Set<StepExecution> job2StepExecutions = job2Result.get(5, TimeUnit.SECONDS);

			assertEquals(2, job1StepExecutions.size());
			for (StepExecution stepExecution : job1StepExecutions) {
				assertEquals(1L, stepExecution.getJobExecution().getId());
			}
			assertEquals(2, job2StepExecutions.size());
			for (StepExecution stepExecution : job2StepExecutions) {
				assertEquals(2L, stepExecution.getJobExecution().getId());
			}
		}
		finally {
			executor.shutdownNow();
		}
	}

	private Message<StepExecution> workerReply(long jobExecutionId, long stepExecutionId, int sequenceNumber,
			int sequenceSize) {
		JobExecution jobExecution = new JobExecution(jobExecutionId, new JobInstance(jobExecutionId, "job"),
				new JobParameters());
		StepExecution stepExecution = new StepExecution(stepExecutionId, "workerStep:partition" + stepExecutionId,
				jobExecution);
		return MessageBuilder.withPayload(stepExecution)
			.setCorrelationId(jobExecutionId + ":workerStep")
			.setSequenceNumber(sequenceNumber)
			.setSequenceSize(sequenceSize)
			.build();
	}

	private StepExecutionSplitter fixedSplitter(long jobExecutionId, Set<Long> partitionStepExecutionIds)
			throws Exception {
		StepExecutionSplitter splitter = mock();
		JobExecution jobExecution = new JobExecution(jobExecutionId, new JobInstance(jobExecutionId, "job"),
				new JobParameters());
		Set<StepExecution> split = new HashSet<>();
		for (Long id : partitionStepExecutionIds) {
			split.add(new StepExecution(id, "workerStep:partition" + id, jobExecution));
		}
		when(splitter.split(org.mockito.ArgumentMatchers.any(StepExecution.class), org.mockito.ArgumentMatchers.eq(1)))
			.thenReturn(split);
		return splitter;
	}

}
