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
package org.springframework.batch.integration.chunk;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for
 * <a href="https://github.com/spring-projects/spring-batch/issues/1372">issue 1372</a>:
 * two writer instances sharing the same reply channel (as recommended for concurrent job
 * instances, since {@link ChunkMessageChannelItemWriter} should be step-scoped) must
 * never receive each other's chunk responses.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkMessageChannelItemWriterConcurrencyTests {

	@Test
	void writersSharingTheSameReplyChannelDoNotCrossChunkResponses() throws Exception {
		DirectChannel replyChannel = new DirectChannel();

		ChunkMessageChannelItemWriter<String> writer1 = newWriter(replyChannel);
		ChunkMessageChannelItemWriter<String> writer2 = newWriter(replyChannel);

		StepExecution stepExecution1 = new StepExecution(1L, "step",
				new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters()));
		StepExecution stepExecution2 = new StepExecution(2L, "step",
				new JobExecution(2L, new JobInstance(2L, "job"), new JobParameters()));

		writer1.beforeStep(stepExecution1);
		writer2.beforeStep(stepExecution2);

		writer1.write(Chunk.of("a"));
		writer2.write(Chunk.of("b"));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> writer1Wait = executor.submit(() -> {
				writer1.getNextResult();
				return null;
			});

			Thread.sleep(200);

			// job 2's reply arrives first, even though writer 1 started waiting first.
			replyChannel.send(chunkResponse(2L, 0));

			Future<?> writer2Wait = executor.submit(() -> {
				writer2.getNextResult();
				return null;
			});

			Thread.sleep(300);
			// job 1's reply finally arrives.
			replyChannel.send(chunkResponse(1L, 0));

			writer1Wait.get(5, TimeUnit.SECONDS);
			writer2Wait.get(5, TimeUnit.SECONDS);
		}
		finally {
			executor.shutdownNow();
		}

		Collection<StepContribution> writer1Contributions = writer1.getStepContributions();
		Collection<StepContribution> writer2Contributions = writer2.getStepContributions();

		assertEquals(1, writer1Contributions.size());
		assertEquals(1, writer2Contributions.size());
	}

	private ChunkMessageChannelItemWriter<String> newWriter(DirectChannel replyChannel) {
		ChunkMessageChannelItemWriter<String> writer = new ChunkMessageChannelItemWriter<>();
		MessagingTemplate messagingTemplate = mock();
		when(messagingTemplate.getReceiveTimeout()).thenReturn(-1L);
		writer.setMessagingOperations(messagingTemplate);
		writer.setReplyChannel(replyChannel);
		return writer;
	}

	private Message<ChunkResponse> chunkResponse(long jobInstanceId, int sequence) {
		StepExecution stepExecution = new StepExecution(jobInstanceId, "step",
				new JobExecution(jobInstanceId, new JobInstance(jobInstanceId, "job"), new JobParameters()));
		ChunkResponse response = new ChunkResponse(sequence, jobInstanceId, stepExecution.createStepContribution());
		return MessageBuilder.withPayload(response).build();
	}

}
