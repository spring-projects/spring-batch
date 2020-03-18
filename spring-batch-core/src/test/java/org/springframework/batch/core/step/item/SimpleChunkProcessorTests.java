/*
 * Copyright 2008-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;

public class SimpleChunkProcessorTests {

	private SimpleChunkProcessor<String, String> processor = new SimpleChunkProcessor<>(
			new ItemProcessor<String, String>() {
				@Nullable
				@Override
				public String process(String item) throws Exception {
					if (item.equals("err")) {
						return null;
					}
					return item;
				}
			}, new ItemWriter<String>() {
				@Override
				public void write(List<? extends String> items) throws Exception {
					if (items.contains("fail")) {
						throw new RuntimeException("Planned failure!");
					}
					list.addAll(items);
				}
			});

	private StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(
			new JobInstance(123L, "job"), new JobParameters())));

	private List<String> list = new ArrayList<>();

	@Before
	public void setUp() {
		list.clear();
	}

	@Test
	public void testProcess() throws Exception {
		Chunk<String> chunk = new Chunk<>();
		chunk.add("foo");
		chunk.add("err");
		chunk.add("bar");
		processor.process(contribution, chunk);
		assertEquals(Arrays.asList("foo", "bar"), list);
		assertEquals(1, contribution.getFilterCount());
		assertEquals(2, contribution.getWriteCount());
	}

}
