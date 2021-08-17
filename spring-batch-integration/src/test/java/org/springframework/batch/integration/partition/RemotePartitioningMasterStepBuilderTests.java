/*
 * Copyright 2018-2021 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RemotePartitioningMasterStepBuilderTests.BatchConfiguration.class})
public class RemotePartitioningMasterStepBuilderTests {

	@Autowired
	private JobRepository jobRepository;

	@Test
	public void inputChannelMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class,
				() -> builder.inputChannel(null));

		// then
		assertThat(expectedException).hasMessage("inputChannel must not be null");
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class,
				() -> builder.outputChannel(null));

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	public void messagingTemplateMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class,
				() -> builder.messagingTemplate(null));

		// then
		assertThat(expectedException).hasMessage("messagingTemplate must not be null");
	}

	@Test
	public void jobExplorerMustNotBeNull() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class,
				() -> builder.jobExplorer(null));

		// then
		assertThat(expectedException).hasMessage("jobExplorer must not be null");
	}

	@Test
	public void pollIntervalMustBeGreaterThanZero() {
		// given
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class,
				() -> builder.pollInterval(-1));

		// then
		assertThat(expectedException).hasMessage("The poll interval must be greater than zero");
	}

	@Test
	public void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step")
				.outputChannel(new DirectChannel())
				.messagingTemplate(new MessagingTemplate());

		// when
		final Exception expectedException = Assert.assertThrows(IllegalStateException.class,
				builder::build);

		// then
		assertThat(expectedException).hasMessage("You must specify either an outputChannel or a messagingTemplate but not both.");
	}

	@Test
	public void testUnsupportedOperationExceptionWhenSpecifyingPartitionHandler() {
		// given
		PartitionHandler partitionHandler = Mockito.mock(PartitionHandler.class);
		final RemotePartitioningManagerStepBuilder builder = new RemotePartitioningManagerStepBuilder("step");

		// when
		final Exception expectedException = Assert.assertThrows(UnsupportedOperationException.class,
				() -> builder.partitionHandler(partitionHandler));

		// then
		assertThat(expectedException).hasMessage("When configuring a manager step " +
				"for remote partitioning using the RemotePartitioningManagerStepBuilder, " +
				"the partition handler will be automatically set to an instance " +
				"of MessageChannelPartitionHandler. The partition handler must " +
				"not be provided in this case.");
	}

	@Test
	public void testMasterStepCreationWhenPollingRepository() {
		// given
		int gridSize = 5;
		int startLimit = 3;
		long timeout = 1000L;
		long pollInterval = 5000L;
		DirectChannel outputChannel = new DirectChannel();
		Partitioner partitioner = Mockito.mock(Partitioner.class);
		StepExecutionAggregator stepExecutionAggregator = (result, executions) -> { };

		// when
		Step step = new RemotePartitioningManagerStepBuilder("masterStep")
				.repository(jobRepository)
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
		Assert.assertNotNull(step);
		Assert.assertEquals(getField(step, "startLimit"), startLimit);
		Assert.assertEquals(getField(step, "jobRepository"), this.jobRepository);
		Assert.assertEquals(getField(step, "stepExecutionAggregator"), stepExecutionAggregator);
		Assert.assertTrue((Boolean) getField(step, "allowStartIfComplete"));

		Object partitionHandler = getField(step, "partitionHandler");
		Assert.assertNotNull(partitionHandler);
		Assert.assertTrue(partitionHandler instanceof MessageChannelPartitionHandler);
		MessageChannelPartitionHandler messageChannelPartitionHandler = (MessageChannelPartitionHandler) partitionHandler;
		Assert.assertEquals(getField(messageChannelPartitionHandler, "gridSize"), gridSize);
		Assert.assertEquals(getField(messageChannelPartitionHandler, "pollInterval"), pollInterval);
		Assert.assertEquals(getField(messageChannelPartitionHandler, "timeout"), timeout);

		Object messagingGateway = getField(messageChannelPartitionHandler, "messagingGateway");
		Assert.assertNotNull(messagingGateway);
		MessagingTemplate messagingTemplate = (MessagingTemplate) messagingGateway;
		Assert.assertEquals(getField(messagingTemplate, "defaultDestination"), outputChannel);
	}

	@Test
	public void testMasterStepCreationWhenAggregatingReplies() {
		// given
		int gridSize = 5;
		int startLimit = 3;
		DirectChannel outputChannel = new DirectChannel();
		Partitioner partitioner = Mockito.mock(Partitioner.class);
		StepExecutionAggregator stepExecutionAggregator = (result, executions) -> { };

		// when
		Step step = new RemotePartitioningManagerStepBuilder("masterStep")
				.repository(jobRepository)
				.outputChannel(outputChannel)
				.partitioner("workerStep", partitioner)
				.gridSize(gridSize)
				.startLimit(startLimit)
				.aggregator(stepExecutionAggregator)
				.allowStartIfComplete(true)
				.build();

		// then
		Assert.assertNotNull(step);
		Assert.assertEquals(getField(step, "startLimit"), startLimit);
		Assert.assertEquals(getField(step, "jobRepository"), this.jobRepository);
		Assert.assertEquals(getField(step, "stepExecutionAggregator"), stepExecutionAggregator);
		Assert.assertTrue((Boolean) getField(step, "allowStartIfComplete"));

		Object partitionHandler = getField(step, "partitionHandler");
		Assert.assertNotNull(partitionHandler);
		Assert.assertTrue(partitionHandler instanceof MessageChannelPartitionHandler);
		MessageChannelPartitionHandler messageChannelPartitionHandler = (MessageChannelPartitionHandler) partitionHandler;
		Assert.assertEquals(getField(messageChannelPartitionHandler, "gridSize"), gridSize);

		Object replyChannel = getField(messageChannelPartitionHandler, "replyChannel");
		Assert.assertNotNull(replyChannel);
		Assert.assertTrue(replyChannel instanceof QueueChannel);

		Object messagingGateway = getField(messageChannelPartitionHandler, "messagingGateway");
		Assert.assertNotNull(messagingGateway);
		MessagingTemplate messagingTemplate = (MessagingTemplate) messagingGateway;
		Assert.assertEquals(getField(messagingTemplate, "defaultDestination"), outputChannel);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
					.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
					.generateUniqueName(true)
					.build();
		}

	}
}
