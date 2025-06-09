/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.core.step;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.observability.BatchStepObservation;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AbstractStep}.
 */
class NonAbstractStepTests {

	AbstractStep tested = new EventTrackingStep();

	StepExecutionListener listener1 = new EventTrackingListener("listener1");

	StepExecutionListener listener2 = new EventTrackingListener("listener2");

	JobRepositoryStub repository = new JobRepositoryStub();

	/**
	 * Sequence of events encountered during step execution.
	 */
	final List<String> events = new ArrayList<>();

	final StepExecution execution = new StepExecution(tested.getName(),
			new JobExecution(new JobInstance(1L, "jobName"), 0L, new JobParameters()), 0L);

	/**
	 * Fills the events list when abstract methods are called.
	 */
	private class EventTrackingStep extends AbstractStep {

		public EventTrackingStep() {
			setBeanName("eventTrackingStep");
		}

		@Override
		protected void open(ExecutionContext ctx) throws Exception {
			events.add("open");
		}

		@Override
		protected void doExecute(StepExecution context) throws Exception {
			assertSame(execution, context);
			events.add("doExecute");
			context.setExitStatus(ExitStatus.COMPLETED);
		}

		@Override
		protected void close(ExecutionContext ctx) throws Exception {
			events.add("close");
		}

	}

	/**
	 * Fills the events list when listener methods are called, prefixed with the name of
	 * the listener.
	 */
	private class EventTrackingListener implements StepExecutionListener {

		private final String name;

		public EventTrackingListener(String name) {
			this.name = name;
		}

		private String getEvent(String event) {
			return name + "#" + event;
		}

		@Override
		public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
			assertSame(execution, stepExecution);
			events.add(getEvent("afterStep(" + stepExecution.getExitStatus().getExitCode() + ")"));
			stepExecution.getExecutionContext().putString("afterStep", "afterStep");
			return stepExecution.getExitStatus();
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			assertSame(execution, stepExecution);
			events.add(getEvent("beforeStep"));
			stepExecution.getExecutionContext().putString("beforeStep", "beforeStep");
		}

	}

	/**
	 * Remembers the last saved values of execution context.
	 */
	private static class JobRepositoryStub extends JobRepositorySupport {

		ExecutionContext saved = new ExecutionContext();

		static long counter = 0;

		@Override
		public void updateExecutionContext(StepExecution stepExecution) {
			Assert.state(stepExecution.getId() != null, "StepExecution must already be saved");
			saved = stepExecution.getExecutionContext();
		}

		@Override
		public void add(StepExecution stepExecution) {
			if (stepExecution.getId() == null) {
				stepExecution.setId(counter);
				counter++;
			}
		}

	}

	@BeforeEach
	void setUp() {
		tested.setJobRepository(repository);
		repository.add(execution);
	}

	@Test
	void testBeanName() {
		AbstractStep step = new AbstractStep() {
			@Override
			protected void doExecute(StepExecution stepExecution) throws Exception {
			}
		};
		assertNull(step.getName());
		step.setBeanName("foo");
		assertEquals("foo", step.getName());
	}

	@Test
	void testName() {
		AbstractStep step = new AbstractStep() {
			@Override
			protected void doExecute(StepExecution stepExecution) throws Exception {
			}
		};
		assertNull(step.getName());
		step.setName("foo");
		assertEquals("foo", step.getName());
		step.setBeanName("bar");
		assertEquals("foo", step.getName());
	}

	/**
	 * Typical step execution scenario.
	 */
	@Test
	void testExecute() throws Exception {
		tested.setStepExecutionListeners(new StepExecutionListener[] { listener1, listener2 });

		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig()
			.observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));
		tested.setObservationRegistry(observationRegistry);

		tested.execute(execution);

		int i = 0;
		assertEquals("listener1#beforeStep", events.get(i++));
		assertEquals("listener2#beforeStep", events.get(i++));
		assertEquals("open", events.get(i++));
		assertEquals("doExecute", events.get(i++));
		assertEquals("listener2#afterStep(COMPLETED)", events.get(i++));
		assertEquals("listener1#afterStep(COMPLETED)", events.get(i++));
		assertEquals("close", events.get(i++));
		assertEquals(7, events.size());

		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());

		assertTrue(repository.saved.containsKey("beforeStep"),
				"Execution context modifications made by listener should be persisted");
		assertTrue(repository.saved.containsKey("afterStep"),
				"Execution context modifications made by listener should be persisted");

		// Observability
		MeterRegistryAssert.assertThat(Metrics.globalRegistry)
			.hasTimerWithNameAndTags(BatchStepObservation.BATCH_STEP_OBSERVATION.getName(),
					Tags.of(Tag.of("error", "none"), Tag.of("spring.batch.step.job.name", "jobName"),
							Tag.of("spring.batch.step.name", "eventTrackingStep"),
							Tag.of("spring.batch.step.status", "COMPLETED")));
	}

	@AfterEach
	void cleanup() {
		Metrics.globalRegistry.clear();
	}

	@Test
	void testFailure() throws Exception {
		tested = new EventTrackingStep() {
			@Override
			protected void doExecute(StepExecution context) throws Exception {
				super.doExecute(context);
				throw new RuntimeException("crash!");
			}
		};
		tested.setJobRepository(repository);
		tested.setStepExecutionListeners(new StepExecutionListener[] { listener1, listener2 });

		tested.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		Throwable expected = execution.getFailureExceptions().get(0);
		assertEquals("crash!", expected.getMessage());

		int i = 0;
		assertEquals("listener1#beforeStep", events.get(i++));
		assertEquals("listener2#beforeStep", events.get(i++));
		assertEquals("open", events.get(i++));
		assertEquals("doExecute", events.get(i++));
		assertEquals("listener2#afterStep(FAILED)", events.get(i++));
		assertEquals("listener1#afterStep(FAILED)", events.get(i++));
		assertEquals("close", events.get(i++));
		assertEquals(7, events.size());

		assertEquals(ExitStatus.FAILED.getExitCode(), execution.getExitStatus().getExitCode());
		String exitDescription = execution.getExitStatus().getExitDescription();
		assertTrue(exitDescription.contains("crash"), "Wrong message: " + exitDescription);

		assertTrue(repository.saved.containsKey("afterStep"),
				"Execution context modifications made by listener should be persisted");
	}

	/**
	 * Exception during business processing.
	 */
	@Test
	void testStoppedStep() throws Exception {
		tested = new EventTrackingStep() {
			@Override
			protected void doExecute(StepExecution context) throws Exception {
				context.setTerminateOnly();
				super.doExecute(context);
			}
		};
		tested.setJobRepository(repository);
		tested.setStepExecutionListeners(new StepExecutionListener[] { listener1, listener2 });

		tested.execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		Throwable expected = execution.getFailureExceptions().get(0);
		assertEquals("JobExecution interrupted.", expected.getMessage());

		int i = 0;
		assertEquals("listener1#beforeStep", events.get(i++));
		assertEquals("listener2#beforeStep", events.get(i++));
		assertEquals("open", events.get(i++));
		assertEquals("doExecute", events.get(i++));
		assertEquals("listener2#afterStep(STOPPED)", events.get(i++));
		assertEquals("listener1#afterStep(STOPPED)", events.get(i++));
		assertEquals("close", events.get(i++));
		assertEquals(7, events.size());

		assertEquals("STOPPED", execution.getExitStatus().getExitCode());

		assertTrue(repository.saved.containsKey("afterStep"),
				"Execution context modifications made by listener should be persisted");
	}

	@Test
	void testStoppedStepWithCustomStatus() throws Exception {
		tested = new EventTrackingStep() {
			@Override
			protected void doExecute(StepExecution context) throws Exception {
				super.doExecute(context);
				context.setTerminateOnly();
				context.setExitStatus(new ExitStatus("FUNNY"));
			}
		};
		tested.setJobRepository(repository);
		tested.setStepExecutionListeners(new StepExecutionListener[] { listener1, listener2 });

		tested.execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		Throwable expected = execution.getFailureExceptions().get(0);
		assertEquals("JobExecution interrupted.", expected.getMessage());

		assertEquals("FUNNY", execution.getExitStatus().getExitCode());

		assertTrue(repository.saved.containsKey("afterStep"),
				"Execution context modifications made by listener should be persisted");
	}

	/**
	 * Exception during business processing.
	 */
	@Test
	void testFailureInSavingExecutionContext() throws Exception {
		tested = new EventTrackingStep() {
			@Override
			protected void doExecute(StepExecution context) throws Exception {
				super.doExecute(context);
			}
		};
		repository = new JobRepositoryStub() {
			@Override
			public void updateExecutionContext(StepExecution stepExecution) {
				throw new RuntimeException("Bad context!");
			}
		};
		tested.setJobRepository(repository);

		tested.execute(execution);
		assertEquals(BatchStatus.UNKNOWN, execution.getStatus());
		Throwable expected = execution.getFailureExceptions().get(0);
		assertEquals("Bad context!", expected.getMessage());

		int i = 0;
		assertEquals("open", events.get(i++));
		assertEquals("doExecute", events.get(i++));
		assertEquals("close", events.get(i++));
		assertEquals(3, events.size());

		assertEquals(ExitStatus.UNKNOWN, execution.getExitStatus());
	}

	/**
	 * JobRepository is a required property.
	 */
	@Test
	void testAfterPropertiesSet() {
		tested.setJobRepository(null);
		assertThrows(IllegalStateException.class, tested::afterPropertiesSet);
	}

}
