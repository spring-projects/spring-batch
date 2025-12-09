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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 */
class RemotePartitioningWorkerStepBuilderTests {

	@Mock
	private Tasklet tasklet;

	@Mock
	private JobRepository jobRepository;

	@Mock
	private PlatformTransactionManager transactionManager;

	@Test
	void inputChannelMustNotBeNull() {
		// given
		final RemotePartitioningWorkerStepBuilder builder = new RemotePartitioningWorkerStepBuilder("step",
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
		final RemotePartitioningWorkerStepBuilder builder = new RemotePartitioningWorkerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.outputChannel(null));

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	void stepLocatorMustNotBeNull() {
		// given
		final RemotePartitioningWorkerStepBuilder builder = new RemotePartitioningWorkerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.stepLocator(null));

		// then
		assertThat(expectedException).hasMessage("stepLocator must not be null");
	}

	@Test
	void beanFactoryMustNotBeNull() {
		// given
		final RemotePartitioningWorkerStepBuilder builder = new RemotePartitioningWorkerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.beanFactory(null));

		// then
		assertThat(expectedException).hasMessage("beanFactory must not be null");
	}

	@Test
	void testMandatoryInputChannel() {
		// given
		final RemotePartitioningWorkerStepBuilder builder = new RemotePartitioningWorkerStepBuilder("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> builder.tasklet(this.tasklet, this.transactionManager));

		// then
		assertThat(expectedException).hasMessage("An InputChannel must be provided");
	}

	@Test
	void testAllMethodsAreOverridden() throws Exception {
		for (Method method : StepBuilder.class.getDeclaredMethods()) {
			if (!Modifier.isPublic(method.getModifiers())) {
				continue;
			}
			try {
				RemotePartitioningWorkerStepBuilder.class.getDeclaredMethod(method.getName(),
						method.getParameterTypes());
			}
			catch (NoSuchMethodException ex) {
				fail(RemotePartitioningWorkerStepBuilder.class.getName() + " should override method [" + method
						+ "] to configure worker integration flow.");
			}
		}
	}

}
