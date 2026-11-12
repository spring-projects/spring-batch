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
package org.springframework.batch.core.step.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the diagnostic logging emitted by {@link ChunkOrientedStep} while a chunk
 * whose write failed with a skippable exception is rolled back and scanned.
 * <p>
 * Rolling back the chunk and re-attempting its items one per transaction is an expected
 * part of the skip algorithm, not a step failure: a job that skips an item and completes
 * successfully must not report it above debug level. Only a failure that actually fails
 * the step is logged as an error.
 *
 * @author Mahmoud Ben Hassine
 * @see <a href="https://github.com/spring-projects/spring-batch/issues/5527">issue
 * 5527</a>
 */
class ChunkOrientedStepSkipLoggingTests {

	/** Skippable: stands in for an exception used as control flow to skip an item. */
	static class DeliberateSkipException extends RuntimeException {

		DeliberateSkipException(String message) {
			super(message);
		}

	}

	/** Not skippable: fails the step. */
	static class FatalWriteException extends RuntimeException {

		FatalWriteException(String message) {
			super(message);
		}

	}

	static class FailingWriter implements ItemWriter<Integer> {

		private final RuntimeException failure;

		FailingWriter(RuntimeException failure) {
			this.failure = failure;
		}

		@Override
		public void write(Chunk<? extends Integer> chunk) {
			for (Integer item : chunk) {
				if (item == 2) {
					throw this.failure;
				}
			}
		}

	}

	static class RecordingSkipListener implements SkipListener<Integer, Integer> {

		private final List<Throwable> writeSkips = new CopyOnWriteArrayList<>();

		@Override
		public void onSkipInWrite(Integer item, Throwable t) {
			this.writeSkips.add(t);
		}

	}

	/** Collects the events logged by {@link ChunkOrientedStep}. */
	static class CapturingAppender extends AbstractAppender {

		private final List<LogEvent> events = new CopyOnWriteArrayList<>();

		CapturingAppender() {
			super("capturing", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			this.events.add(event.toImmutable());
		}

		List<LogEvent> eventsAtOrAbove(Level level) {
			return this.events.stream().filter(event -> event.getLevel().isMoreSpecificThan(level)).toList();
		}

	}

	private CapturingAppender appender;

	private Logger stepLogger;

	private Level previousLevel;

	@BeforeEach
	void setUp() {
		this.appender = new CapturingAppender();
		this.appender.start();
		LoggerContext loggerContext = LoggerContext.getContext(false);
		this.stepLogger = loggerContext.getLogger(ChunkOrientedStep.class.getName());
		this.previousLevel = this.stepLogger.getLevel();
		// debug logging is enabled to make sure the expected transitions are still
		// reported to whoever asks for them
		this.stepLogger.setLevel(Level.DEBUG);
		this.stepLogger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() {
		this.stepLogger.removeAppender(this.appender);
		this.stepLogger.setLevel(this.previousLevel);
		this.appender.stop();
	}

	@Test
	void testSkippedWriteIsNotLoggedAboveDebug() throws Exception {
		// given: a skippable write failure and no retryable exception configured, so the
		// retry policy allows no retry at all
		RecordingSkipListener skipListener = new RecordingSkipListener();
		Step step = step(new FailingWriter(new DeliberateSkipException("skip item 2")), DeliberateSkipException.class,
				skipListener);

		// when
		JobExecution jobExecution = run(step);

		// then: the item is skipped and the step completes
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(1, skipListener.writeSkips.size());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// and the expected rollback and scan are only reported at debug level
		assertEquals(List.of(), this.appender.eventsAtOrAbove(Level.INFO).stream().map(Object::toString).toList());
		assertTrue(this.appender.eventsAtOrAbove(Level.DEBUG).size() > 0,
				"the skip/scan transitions should still be reported at debug level");
	}

	@Test
	void testNonSkippableWriteFailureIsStillLoggedAsAnError() throws Exception {
		// given
		RecordingSkipListener skipListener = new RecordingSkipListener();
		Step step = step(new FailingWriter(new FatalWriteException("cannot skip item 2")),
				DeliberateSkipException.class, skipListener);

		// when
		JobExecution jobExecution = run(step);

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, skipListener.writeSkips.size());
		assertTrue(this.appender.eventsAtOrAbove(Level.ERROR).size() > 0,
				"a write failure that fails the step should be logged as an error");
	}

	private Step step(ItemWriter<Integer> writer, Class<? extends Throwable> skippableException,
			SkipListener<Integer, Integer> skipListener) {
		return new ChunkOrientedStepBuilder<Integer, Integer>("step", jobRepository(), 3)
			.reader(new ListItemReader<>(List.of(1, 2, 3)))
			.writer(writer)
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.skip(skippableException)
			.skipLimit(10)
			.skipListener(skipListener)
			.build();
	}

	private final PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	private AnnotationConfigApplicationContext context;

	private JobRepository jobRepository() {
		if (this.context == null) {
			this.context = new AnnotationConfigApplicationContext(BatchConfiguration.class);
		}
		return this.context.getBean(JobRepository.class);
	}

	private JobExecution run(Step step) throws Exception {
		JobRepository jobRepository = jobRepository();
		Job job = new JobBuilder("job", jobRepository).start(step).build();
		JobOperator jobOperator = this.context.getBean(JobOperator.class);
		return jobOperator.start(job, new JobParameters());
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration {

	}

}
