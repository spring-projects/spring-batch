/*
 * Copyright 2018-2025 the original author or authors.
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

package org.springframework.batch.integration.partition;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.partition.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { RemotePartitioningManagerStepBuilderTests.BatchConfiguration.class })
class RemotePartitioningManagerStepBuilderTests {

	@Autowired
	private JobRepository jobRepository;

	@Test
	void inputChannelMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.inputChannel(null));

		// then
		assertThat(expectedException).hasMessage("inputChannel must not be null");
	}

	@Test
	void outputChannelMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.outputChannel(null));

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	void messagingTemplateMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.messagingTemplate(null));

		// then
		assertThat(expectedException).hasMessage("messagingTemplate must not be null");
	}

	@Test
	void jobExplorerMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.jobExplorer(null));

		// then
		assertThat(expectedException).hasMessage("jobExplorer must not be null");
	}

	@Test
	void pollIntervalMustBeGreaterThanZero() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.pollInterval(-1));

		// then
		assertThat(expectedException).hasMessage("The poll interval must be greater than zero");
	}

	@Test
	void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository)
			.outputChannel(new DirectChannel())
			.messagingTemplate(new MessagingTemplate());

		// when
		final Exception expectedException = assertThrows(IllegalStateException.class, builder::build);

		// then
		assertThat(expectedException)
			.hasMessage("You must specify either an outputChannel or a messagingTemplate but not both.");
	}

	@Test
	void testUnsupportedOperationExceptionWhenSpecifyingPartitionHandler() {
		// given
		PartitionHandler partitionHandler = Mockito.mock();
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(UnsupportedOperationException.class,
				() -> builder.partitionHandler(partitionHandler));

		// then
		assertThat(expectedException).hasMessage("When configuring a manager step "
				+ "for remote partitioning using the RemotePartitioningManagerStepBuilder, "
				+ "the partition handler will be automatically set to an instance "
				+ "of MessageChannelPartitionHandler. The partition handler must " + "not be provided in this case.");
	}

	@Test
	void testManagerStepCreationWhenPollingRepository() {
		// given
		int gridSize = 5;
		int startLimit = 3;
		long timeout = 1000L;
		long pollInterval = 5000L;
		DirectChannel outputChannel = new DirectChannel();
		Partitioner partitioner = Mockito.mock();
		StepExecutionAggregator stepExecutionAggregator = (result, executions) -> {
		};

		// when
		Step step = new RemotePartitioningManagerStepBuilder("managerStep", this.jobRepository)
			.outputChannel(outputChannel)
			.partitioner("workerStep", partitioner)
			.gridSize(gridSize)
			.pollInterval(pollInterval)
			.timeout(timeout)
			.startLimit(startLimit)
			.aggregator(stepExecutionAggregator)
			.allowStartIfComplete(true)
			.build();

		// then
		assertNotNull(step);
		assertEquals(getField(step, "startLimit"), startLimit);
		assertEquals(getField(step, "jobRepository"), this.jobRepository);
		assertEquals(getField(step, "stepExecutionAggregator"), stepExecutionAggregator);
		assertTrue((Boolean) getField(step, "allowStartIfComplete"));

		Object partitionHandler = getField(step, "partitionHandler");
		assertNotNull(partitionHandler);
		assertTrue(partitionHandler instanceof MessageChannelPartitionHandler);
		MessageChannelPartitionHandler messageChannelPartitionHandler = (MessageChannelPartitionHandler) partitionHandler;
		assertEquals(getField(messageChannelPartitionHandler, "gridSize"), gridSize);
		assertEquals(getField(messageChannelPartitionHandler, "pollInterval"), pollInterval);
		assertEquals(getField(messageChannelPartitionHandler, "timeout"), timeout);

		Object messagingGateway = getField(messageChannelPartitionHandler, "messagingGateway");
		assertNotNull(messagingGateway);
		MessagingTemplate messagingTemplate = (MessagingTemplate) messagingGateway;
		assertEquals(getField(messagingTemplate, "defaultDestination"), outputChannel);
	}

	@Test
	void testManagerStepCreationWhenAggregatingReplies() {
		// given
		int gridSize = 5;
		int startLimit = 3;
		DirectChannel outputChannel = new DirectChannel();
		Partitioner partitioner = Mockito.mock();
		StepExecutionAggregator stepExecutionAggregator = (result, executions) -> {
		};

		// when
		Step step = new RemotePartitioningManagerStepBuilder("managerStep", this.jobRepository)
			.outputChannel(outputChannel)
			.partitioner("workerStep", partitioner)
			.gridSize(gridSize)
			.startLimit(startLimit)
			.aggregator(stepExecutionAggregator)
			.allowStartIfComplete(true)
			.build();

		// then
		assertNotNull(step);
		assertEquals(getField(step, "startLimit"), startLimit);
		assertEquals(getField(step, "jobRepository"), this.jobRepository);
		assertEquals(getField(step, "stepExecutionAggregator"), stepExecutionAggregator);
		assertTrue((Boolean) getField(step, "allowStartIfComplete"));

		Object partitionHandler = getField(step, "partitionHandler");
		assertNotNull(partitionHandler);
		assertTrue(partitionHandler instanceof MessageChannelPartitionHandler);
		MessageChannelPartitionHandler messageChannelPartitionHandler = (MessageChannelPartitionHandler) partitionHandler;
		assertEquals(getField(messageChannelPartitionHandler, "gridSize"), gridSize);

		Object replyChannel = getField(messageChannelPartitionHandler, "replyChannel");
		assertNotNull(replyChannel);
		assertTrue(replyChannel instanceof QueueChannel);

		Object messagingGateway = getField(messageChannelPartitionHandler, "messagingGateway");
		assertNotNull(messagingGateway);
		MessagingTemplate messagingTemplate = (MessagingTemplate) messagingGateway;
		assertEquals(getField(messagingTemplate, "defaultDestination"), outputChannel);
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
