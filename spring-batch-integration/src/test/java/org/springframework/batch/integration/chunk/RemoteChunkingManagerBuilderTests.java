/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.ChunkOrientedStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { RemoteChunkingManagerBuilderTests.BatchConfiguration.class })
class RemoteChunkingManagerBuilderTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final DirectChannel inputChannel = new DirectChannel();

	private final DirectChannel outputChannel = new DirectChannel();

	private final ItemReader<String> itemReader = new ListItemReader<>(Arrays.asList("a", "b", "c", "d", "e"));

	@Test
	void inputChannelMustNotBeNull() {
		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerBuilder("step", this.jobRepository).inputChannel(null));

		// then
		assertThat(expectedException).hasMessage("inputChannel must not be null");
	}

	@Test
	void outputChannelMustNotBeNull() {
		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerBuilder("step", this.jobRepository).outputChannel(null));

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	void messagingTemplateMustNotBeNull() {
		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerBuilder("step", this.jobRepository).messagingTemplate(null));

		// then
		assertThat(expectedException).hasMessage("messagingTemplate must not be null");
	}

	@Test
	void maxWaitTimeoutsMustBeGreaterThanZero() {
		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerBuilder("step", this.jobRepository).maxWaitTimeouts(-1));

		// then
		assertThat(expectedException).hasMessage("maxWaitTimeouts must be greater than zero");
	}

	@Test
	void throttleLimitMustBeGreaterThanZero() {
		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerBuilder("step", this.jobRepository).throttleLimit(-1L));

		// then
		assertThat(expectedException).hasMessage("throttleLimit must be greater than zero");
	}

	@Test
	void inputChannelIsMandatory() {
		// given
		RemoteChunkingManagerBuilder builder = new RemoteChunkingManagerBuilder("step", this.jobRepository)
			.outputChannel(this.outputChannel);

		// when
		Exception expectedException = assertThrows(IllegalArgumentException.class, () -> builder.chunk(2));

		// then
		assertThat(expectedException).hasMessage("An InputChannel must be provided");
	}

	@Test
	void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemoteChunkingManagerBuilder builder = new RemoteChunkingManagerBuilder("step", this.jobRepository)
			.inputChannel(this.inputChannel);

		// when
		Exception expectedException = assertThrows(IllegalStateException.class, () -> builder.chunk(2));

		// then
		assertThat(expectedException).hasMessage("You must specify either an outputChannel or a messagingTemplate.");
	}

	@Test
	void outputChannelAndMessagingTemplateAreMutuallyExclusive() {
		// given
		RemoteChunkingManagerBuilder builder = new RemoteChunkingManagerBuilder("step", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.messagingTemplate(new MessagingTemplate());

		// when
		Exception expectedException = assertThrows(IllegalStateException.class, () -> builder.chunk(2));

		// then
		assertThat(expectedException)
			.hasMessage("You must specify either an outputChannel or a messagingTemplate but not both.");
	}

	@Test
	void testManagerStepCreation() {
		// when
		ChunkOrientedStep<String, String> step = new RemoteChunkingManagerBuilder("managerStep", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.<String, String>chunk(3)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.build();

		// then
		assertEquals("managerStep", step.getName());
		assertEquals(3, ReflectionTestUtils.getField(step, "chunkSize"));
		assertEquals(this.jobRepository, ReflectionTestUtils.getField(step, "jobRepository"));
		assertSame(this.itemReader, ReflectionTestUtils.getField(step, "itemReader"));
		Object itemWriter = ReflectionTestUtils.getField(step, "itemWriter");
		assertInstanceOf(ChunkMessageChannelItemWriter.class, itemWriter);
		assertSame(this.inputChannel, ReflectionTestUtils.getField(itemWriter, "replyChannel"));
		assertEquals(40, ReflectionTestUtils.getField(itemWriter, "maxWaitTimeouts"));
		assertEquals(6L, ReflectionTestUtils.getField(itemWriter, "throttleLimit"));
		Object messagingTemplate = ReflectionTestUtils.getField(itemWriter, "messagingGateway");
		assertSame(this.outputChannel, ReflectionTestUtils.getField(messagingTemplate, "defaultDestination"));
	}

	@Test
	void testCustomSettingsAreAppliedToTheItemWriter() {
		// given
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultChannel(this.outputChannel);

		// when
		ChunkOrientedStep<String, String> step = new RemoteChunkingManagerBuilder("managerStep", this.jobRepository)
			.inputChannel(this.inputChannel)
			.messagingTemplate(messagingTemplate)
			.maxWaitTimeouts(10)
			.throttleLimit(3)
			.<String, String>chunk(2)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.build();

		// then
		Object itemWriter = ReflectionTestUtils.getField(step, "itemWriter");
		assertSame(messagingTemplate, ReflectionTestUtils.getField(itemWriter, "messagingGateway"));
		assertEquals(10, ReflectionTestUtils.getField(itemWriter, "maxWaitTimeouts"));
		assertEquals(3L, ReflectionTestUtils.getField(itemWriter, "throttleLimit"));
	}

	@Test
	void testPropertiesOfTheParentBuilderAreCarriedOver() throws Exception {
		// given
		StepExecutionListener stepExecutionListener = mock();

		// when: methods of StepBuilder return a StepBuilder, chunk(int) is still the
		// overridden one
		StepBuilder stepBuilder = new RemoteChunkingManagerBuilder("managerStep", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.startLimit(3)
			.listener(stepExecutionListener);
		ChunkOrientedStep<String, String> step = stepBuilder.<String, String>chunk(2)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.build();

		// then
		assertEquals(3, ReflectionTestUtils.getField(step, "startLimit"));
		assertInstanceOf(ChunkMessageChannelItemWriter.class, ReflectionTestUtils.getField(step, "itemWriter"));

		StepExecution stepExecution = createStepExecution("managerStep");
		step.execute(stepExecution);
		verify(stepExecutionListener).beforeStep(any());
	}

	@Test
	void testChunksAreSentToWorkersAndResultsAreAggregated() throws Exception {
		// given
		List<String> written = new ArrayList<>();
		subscribeWorker(written, null);

		ChunkOrientedStep<String, String> step = new RemoteChunkingManagerBuilder("managerStep", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.<String, String>chunk(2)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.build();
		StepExecution stepExecution = createStepExecution("managerStep");

		// when
		step.execute(stepExecution);

		// then
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(Arrays.asList("a", "b", "c", "d", "e"), written);
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(3, stepExecution.getCommitCount());
	}

	@Test
	void testFailureOnWorkerSideFailsTheManagerStep() throws Exception {
		// given
		List<String> written = new ArrayList<>();
		subscribeWorker(written, "c");

		ChunkOrientedStep<String, String> step = new RemoteChunkingManagerBuilder("managerStep", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.<String, String>chunk(2)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.build();
		StepExecution stepExecution = createStepExecution("managerStep");

		// when
		step.execute(stepExecution);

		// then
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertThat(written).contains("a", "b").doesNotContain("c", "d");
	}

	/*
	 * Simulates a worker: requests received on the output channel are handled by a
	 * ChunkProcessorChunkRequestHandler and replies are sent on the input channel. The
	 * worker fails when it receives the item passed as failOn.
	 */
	@SuppressWarnings("unchecked")
	private void subscribeWorker(List<String> written, String failOn) {
		ChunkProcessorChunkRequestHandler<String> handler = new ChunkProcessorChunkRequestHandler<>();
		handler.setChunkProcessor((chunk, contribution) -> {
			if (failOn != null && chunk.getItems().contains(failOn)) {
				throw new IllegalStateException("Unable to process " + failOn);
			}
			written.addAll(chunk.getItems());
			contribution.incrementWriteCount(chunk.size());
		});
		this.outputChannel.subscribe(message -> {
			try {
				ChunkResponse response = handler.handle((ChunkRequest<String>) message.getPayload());
				this.inputChannel.send(new GenericMessage<>(response));
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
	}

	private StepExecution createStepExecution(String stepName) {
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = this.jobRepository.createJobInstance("job-" + UUID.randomUUID(), jobParameters);
		JobExecution jobExecution = this.jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		return this.jobRepository.createStepExecution(stepName, jobExecution);
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class BatchConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript(DatabaseType.HSQL.getProductSchemaDrop())
				.addScript(DatabaseType.HSQL.getProductSchema())
				.generateUniqueName(true)
				.build();
		}

		@Bean
		JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
