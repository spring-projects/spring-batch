/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.ChunkOrientedStep;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ChunkOrientedStepBuilder}.
 *
 * @since 6.0
 */
class ChunkOrientedStepBuilderTests {

	@Test
	void testRetryLimitWithoutRetryDoesNotRetryErrors() {
		// Given: ItemProcessor that throws OutOfMemoryError
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			attempts.incrementAndGet();
			throw new OutOfMemoryError("Simulated OOM");
		};

		ChunkOrientedStepBuilder<String, String> builder = new ChunkOrientedStepBuilder<>("testStep",
				mock(JobRepository.class), 2);
		builder.reader(new ListItemReader<>(List.of("item1"))).processor(processor).writer(items -> {
		}).transactionManager(mock(JdbcTransactionManager.class)).faultTolerant().retryLimit(3);

		ChunkOrientedStep<String, String> step = builder.build();

		// When & Then: Should fail immediately without retry
		// Currently this test FAILS (bug exists - Error is retried)
		// After fix: Should PASS (Error is not retried)
		assertThrows(Throwable.class, () -> {
			try {
				step.execute(null);
			}
			catch (Exception e) {
				throw e.getCause() != null ? e.getCause() : e;
			}
		});

		// Bug: currently attempts.get() will be 4 (1 initial + 3 retries)
		// After fix: attempts.get() should be 1 (no retry)
		assertEquals(1, attempts.get(),
				"OutOfMemoryError should not be retried. Expected 1 attempt, but got " + attempts.get());
	}

	@Test
	void testRetryLimitWithoutRetryRetriesExceptions() {
		// Given: ItemProcessor that fails first 2 times with Exception
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			if (attempts.incrementAndGet() < 3) {
				throw new RuntimeException("Temporary failure");
			}
			return item.toUpperCase();
		};

		List<String> writtenItems = new ArrayList<>();
		ChunkOrientedStepBuilder<String, String> builder = new ChunkOrientedStepBuilder<>("testStep",
				mock(JobRepository.class), 2);
		builder.reader(new ListItemReader<>(List.of("item1")))
			.processor(processor)
			.writer(writtenItems::addAll)
			.transactionManager(mock(JdbcTransactionManager.class))
			.faultTolerant()
			.retryLimit(3);

		ChunkOrientedStep<String, String> step = builder.build();

		// When: Execute step
		// Then: Should succeed after 2 retries
		step.execute(null);

		// Should have retried 2 times (total 3 attempts)
		assertEquals(3, attempts.get(), "Should retry RuntimeException");
		assertEquals(List.of("ITEM1"), writtenItems, "Item should be processed successfully");
	}

	@Test
	void testExplicitRetryConfigurationTakesPrecedence() {
		// Given: Explicit retry configuration for IllegalStateException only
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			attempts.incrementAndGet();
			throw new RuntimeException("This should not be retried");
		};

		ChunkOrientedStepBuilder<String, String> builder = new ChunkOrientedStepBuilder<>("testStep",
				mock(JobRepository.class), 2);
		builder.reader(new ListItemReader<>(List.of("item1"))).processor(processor).writer(items -> {
		})
			.transactionManager(mock(JdbcTransactionManager.class))
			.faultTolerant()
			.retry(IllegalStateException.class)
			.retryLimit(3);

		ChunkOrientedStep<String, String> step = builder.build();

		// When & Then: Should fail immediately without retry
		// because RuntimeException is not in the explicit retry list
		assertThrows(Throwable.class, () -> {
			try {
				step.execute(null);
			}
			catch (Exception e) {
				throw e.getCause() != null ? e.getCause() : e;
			}
		});

		// Should not retry (only 1 attempt)
		assertEquals(1, attempts.get(),
				"RuntimeException should not be retried when only IllegalStateException is configured");
	}

}
