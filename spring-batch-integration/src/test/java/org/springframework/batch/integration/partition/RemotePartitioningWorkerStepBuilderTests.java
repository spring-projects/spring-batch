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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Mahmoud Ben Hassine
 */
public class RemotePartitioningWorkerStepBuilderTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private Tasklet tasklet;

	@Test
	public void inputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("inputChannel must not be null");

		// when
		new RemotePartitioningWorkerStepBuilder("step").inputChannel(null);

		// then
		// expected exception
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("outputChannel must not be null");

		// when
		new RemotePartitioningWorkerStepBuilder("step").outputChannel(null);

		// then
		// expected exception
	}

	@Test
	public void jobExplorerMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("jobExplorer must not be null");

		// when
		new RemotePartitioningWorkerStepBuilder("step").jobExplorer(null);

		// then
		// expected exception
	}

	@Test
	public void stepLocatorMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("stepLocator must not be null");

		// when
		new RemotePartitioningWorkerStepBuilder("step").stepLocator(null);

		// then
		// expected exception
	}

	@Test
	public void beanFactoryMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("beanFactory must not be null");

		// when
		new RemotePartitioningWorkerStepBuilder("step").beanFactory(null);

		// then
		// expected exception
	}

	@Test
	public void testMandatoryInputChannel() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An InputChannel must be provided");

		// when
		new RemotePartitioningWorkerStepBuilder("step").tasklet(this.tasklet);

		// then
		// expected exception
	}

	@Test
	public void testMandatoryJobExplorer() {
		// given
		DirectChannel inputChannel = new DirectChannel();
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("A JobExplorer must be provided");

		// when
		new RemotePartitioningWorkerStepBuilder("step")
				.inputChannel(inputChannel)
				.tasklet(this.tasklet);

		// then
		// expected exception
	}

}
