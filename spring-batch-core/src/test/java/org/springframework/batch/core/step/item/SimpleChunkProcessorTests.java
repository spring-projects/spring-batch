/*
 * Copyright 2008-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

class SimpleChunkProcessorTests {

	private final SimpleChunkProcessor<String, String> processor = new SimpleChunkProcessor<>(new ItemProcessor<>() {

		@Override
		public @Nullable String process(String item) throws Exception {
			if (item.equals("err")) {
				return null;
			}
			return item;
		}
	}, new ItemWriter<>() {
		@Override
		public void write(Chunk<? extends String> chunk) throws Exception {
			if (chunk.getItems().contains("fail")) {
				throw new RuntimeException("Planned failure!");
			}
			Chunk<? extends String>.ChunkIterator iterator = chunk.iterator();
			while (iterator.hasNext()) {
				String item = iterator.next();
				if (item.equals("skip")) {
					iterator.remove((Exception) null);
				}
				else {
					list.add(item);
				}
			}
		}
	});

	private final StepContribution contribution = new StepContribution(
			new StepExecution("foo", new JobExecution(1L, new JobInstance(123L, "job"), new JobParameters())));

	private final List<String> list = new ArrayList<>();

	@BeforeEach
	void setUp() {
		list.clear();
	}

	@Test
	void testProcess() throws Exception {
		Chunk<String> chunk = new Chunk<>();
		chunk.add("foo");
		chunk.add("err");
		chunk.add("bar");
		processor.process(contribution, chunk);
		assertEquals(Arrays.asList("foo", "bar"), list);
		assertEquals(1, contribution.getFilterCount());
		assertEquals(2, contribution.getWriteCount());
	}

	@Test
	void testTransform() throws Exception {
		Chunk<String> inputs = new Chunk<>();
		inputs.add("foo");
		inputs.add("bar");
		inputs.setEnd();
		Chunk<String> outputs = processor.transform(contribution, inputs);
		assertEquals(Arrays.asList("foo", "bar"), outputs.getItems());
		assertTrue(outputs.isEnd());
	}

	@Test
	void testWriteWithSkip() throws Exception {
		Chunk<String> inputs = new Chunk<>();
		inputs.add("foo");
		inputs.add("skip");
		inputs.add("bar");
		processor.process(contribution, inputs);
		assertEquals(2, contribution.getWriteCount());
		assertEquals(1, contribution.getWriteSkipCount());
	}

}
