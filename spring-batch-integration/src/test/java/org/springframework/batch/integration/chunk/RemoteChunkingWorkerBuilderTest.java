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
package org.springframework.batch.integration.chunk;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;

/**
 * @author Mahmoud Ben Hassine
 */
public class RemoteChunkingWorkerBuilderTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private ItemProcessor<String, String> itemProcessor = new PassThroughItemProcessor<>();
	private ItemWriter<String> itemWriter = items -> { };

	@Test
	public void itemProcessorMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("itemProcessor must not be null");

		// when
		IntegrationFlow integrationFlow = new RemoteChunkingWorkerBuilder<String, String>()
				.itemProcessor(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void itemWriterMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("itemWriter must not be null");

		// when
		IntegrationFlow integrationFlow = new RemoteChunkingWorkerBuilder<String, String>()
				.itemWriter(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void inputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("inputChannel must not be null");

		// when
		IntegrationFlow integrationFlow = new RemoteChunkingWorkerBuilder<String, String>()
				.inputChannel(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("outputChannel must not be null");

		// when
		IntegrationFlow integrationFlow = new RemoteChunkingWorkerBuilder<String, String>()
				.outputChannel(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void testMandatoryItemWriter() {
		// given
		RemoteChunkingWorkerBuilder<String, String> builder = new RemoteChunkingWorkerBuilder<>();

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An ItemWriter must be provided");

		// when
		builder.build();

		// then
		// expected exception
	}

	@Test
	public void testMandatoryInputChannel() {
		// given
		RemoteChunkingWorkerBuilder<String, String> builder = new RemoteChunkingWorkerBuilder<String, String>()
				.itemWriter(items -> { });

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An InputChannel must be provided");

		// when
		builder.build();

		// then
		// expected exception
	}

	@Test
	public void testMandatoryOutputChannel() {
		// given
		RemoteChunkingWorkerBuilder<String, String> builder = new RemoteChunkingWorkerBuilder<String, String>()
				.itemWriter(items -> { })
				.inputChannel(new DirectChannel());

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An OutputChannel must be provided");

		// when
		builder.build();

		// then
		// expected exception
	}

	@Test
	public void testIntegrationFlowCreation() {
		// given
		DirectChannel inputChannel = new DirectChannel();
		DirectChannel outputChannel = new DirectChannel();
		RemoteChunkingWorkerBuilder<String, String> builder = new RemoteChunkingWorkerBuilder<String, String>()
				.itemProcessor(this.itemProcessor)
				.itemWriter(this.itemWriter)
				.inputChannel(inputChannel)
				.outputChannel(outputChannel);

		// when
		IntegrationFlow integrationFlow = builder.build();

		// then
		Assert.assertNotNull(integrationFlow);
	}

}
