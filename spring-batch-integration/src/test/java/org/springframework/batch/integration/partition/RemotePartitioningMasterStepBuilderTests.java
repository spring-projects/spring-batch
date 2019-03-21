/*
 * Copyright 2018 the original author or authors.
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RemotePartitioningMasterStepBuilderTests.BatchConfiguration.class})
public class RemotePartitioningMasterStepBuilderTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Autowired
	private JobRepository jobRepository;

	@Test
	public void inputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("inputChannel must not be null");

		// when
		new RemotePartitioningMasterStepBuilder("step").inputChannel(null);

		// then
		// expected exception
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("outputChannel must not be null");

		// when
		new RemotePartitioningMasterStepBuilder("step").outputChannel(null);

		// then
		// expected exception
	}

	@Test
	public void messagingTemplateMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("messagingTemplate must not be null");

		// when
		new RemotePartitioningMasterStepBuilder("step").messagingTemplate(null);

		// then
		// expected exception
	}

	@Test
	public void jobExplorerMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("jobExplorer must not be null");

		// when
		new RemotePartitioningMasterStepBuilder("step").jobExplorer(null);

		// then
		// expected exception
	}

	@Test
	public void pollIntervalMustBeGreaterThanZero() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The poll interval must be greater than zero");

		// when
		new RemotePartitioningMasterStepBuilder("step").pollInterval(-1);

		// then
		// expected exception
	}

	@Test
	public void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemotePartitioningMasterStepBuilder builder = new RemotePartitioningMasterStepBuilder("step")
				.outputChannel(new DirectChannel())
				.messagingTemplate(new MessagingTemplate());

		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("You must specify either an outputChannel or a messagingTemplate but not both.");

		// when
		Step step = builder.build();

		// then
		// expected exception
	}

	@Test
	public void testUnsupportedOperationExceptionWhenSpecifyingPartitionHandler() {
		// given
		PartitionHandler partitionHandler = Mockito.mock(PartitionHandler.class);
		this.expectedException.expect(UnsupportedOperationException.class);
		this.expectedException.expectMessage("When configuring a master step " +
				"for remote partitioning using the RemotePartitioningMasterStepBuilder, " +
				"the partition handler will be automatically set to an instance " +
				"of MessageChannelPartitionHandler. The partition handler must " +
				"not be provided in this case.");

		// when
		new RemotePartitioningMasterStepBuilder("step").partitionHandler(partitionHandler);

		// then
		// expected exception
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
		Step step = new RemotePartitioningMasterStepBuilder("masterStep")
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
		Step step = new RemotePartitioningMasterStepBuilder("masterStep")
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

	}
}
