/*
 * Copyright 2008-2024 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;

class SimpleChunkProcessorTests {

	private final SimpleChunkProcessor<String, String> processor = new SimpleChunkProcessor<>(new ItemProcessor<>() {
		@Nullable
		@Override
		public String process(String item) throws Exception {
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
			list.addAll(chunk.getItems());
		}
	});

	private final StepContribution contribution = new StepContribution(
			new StepExecution("foo", new JobExecution(new JobInstance(123L, "job"), new JobParameters())));

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

}
