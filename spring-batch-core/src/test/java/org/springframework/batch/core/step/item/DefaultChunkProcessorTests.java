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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultChunkProcessor}.
 *
 * @author Mahmoud Ben Hassine
 */
class DefaultChunkProcessorTests {

	private final List<String> written = new ArrayList<>();

	private final ItemWriter<String> itemWriter = chunk -> chunk.getItems().forEach(this.written::add);

	private final StepContribution contribution = new StepContribution(
			new StepExecution(1, "step", new JobExecution(1, new JobInstance(1, "job"), new JobParameters())));

	@Test
	void testItemWriterIsMandatory() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultChunkProcessor<String, String>(null, null));
	}

	@Test
	void testItemsAreProcessedAndWritten() throws Exception {
		// given
		ItemProcessor<String, String> itemProcessor = String::toUpperCase;
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(itemProcessor,
				this.itemWriter);

		// when
		chunkProcessor.process(Chunk.of("a", "b", "c"), this.contribution);

		// then
		assertEquals(List.of("A", "B", "C"), this.written);
		assertEquals(3, this.contribution.getWriteCount());
		assertEquals(0, this.contribution.getFilterCount());
	}

	@Test
	void testItemProcessorCanChangeTheType() throws Exception {
		// given
		List<Integer> lengths = new ArrayList<>();
		ItemWriter<Integer> lengthWriter = chunk -> lengths.addAll(chunk.getItems());
		DefaultChunkProcessor<String, Integer> chunkProcessor = new DefaultChunkProcessor<>(String::length,
				lengthWriter);

		// when
		chunkProcessor.process(Chunk.of("a", "bb", "ccc"), this.contribution);

		// then
		assertEquals(List.of(1, 2, 3), lengths);
		assertEquals(3, this.contribution.getWriteCount());
	}

	@Test
	void testItemsAreWrittenAsIsWithoutItemProcessor() throws Exception {
		// given
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(this.itemWriter);

		// when
		chunkProcessor.process(Chunk.of("a", "b"), this.contribution);

		// then
		assertEquals(List.of("a", "b"), this.written);
		assertEquals(2, this.contribution.getWriteCount());
		assertEquals(0, this.contribution.getFilterCount());
	}

	@Test
	void testItemsFilteredByTheItemProcessorAreNotWritten() throws Exception {
		// given
		ItemProcessor<String, String> itemProcessor = item -> "b".equals(item) ? null : item;
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(itemProcessor,
				this.itemWriter);

		// when
		chunkProcessor.process(Chunk.of("a", "b", "c"), this.contribution);

		// then
		assertEquals(List.of("a", "c"), this.written);
		assertEquals(2, this.contribution.getWriteCount());
		assertEquals(1, this.contribution.getFilterCount());
	}

	@Test
	void testEmptyChunkIsIgnored() throws Exception {
		// given
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(this.itemWriter);

		// when
		chunkProcessor.process(new Chunk<>(), this.contribution);

		// then
		assertTrue(this.written.isEmpty());
		assertEquals(0, this.contribution.getWriteCount());
		assertEquals(0, this.contribution.getFilterCount());
	}

	@Test
	void testItemProcessorFailureIsRethrownAndNothingIsWritten() {
		// given
		ItemProcessor<String, String> itemProcessor = item -> {
			if ("b".equals(item)) {
				throw new IllegalStateException("expected");
			}
			return item;
		};
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(itemProcessor,
				this.itemWriter);

		// when
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> chunkProcessor.process(Chunk.of("a", "b", "c"), this.contribution));

		// then
		assertEquals("expected", exception.getMessage());
		assertTrue(this.written.isEmpty());
		assertEquals(0, this.contribution.getWriteCount());
	}

	@Test
	void testItemWriterFailureIsRethrownAndWriteCountIsNotIncremented() {
		// given
		ItemWriter<String> failingWriter = chunk -> {
			throw new IllegalStateException("expected");
		};
		DefaultChunkProcessor<String, String> chunkProcessor = new DefaultChunkProcessor<>(failingWriter);

		// when
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> chunkProcessor.process(Chunk.of("a", "b"), this.contribution));

		// then
		assertEquals("expected", exception.getMessage());
		assertEquals(0, this.contribution.getWriteCount());
		assertFalse(this.written.contains("a"));
	}

}
