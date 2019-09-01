/*
 * Copyright 2010-2019 the original author or authors.
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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.lang.Nullable;

public class FaultTolerantChunkProviderTests {

	private FaultTolerantChunkProvider<String> provider;

	private StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(
			new JobInstance(123L, "job"), new JobParameters())));

	@Test
	public void testProvide() throws Exception {
		provider = new FaultTolerantChunkProvider<>(new ListItemReader<>(Arrays.asList("foo", "bar")),
				new RepeatTemplate());
		Chunk<String> chunk = provider.provide(contribution);
		assertNotNull(chunk);
		assertEquals(2, chunk.getItems().size());
	}

	@Test
	public void testProvideWithOverflow() throws Exception {
		provider = new FaultTolerantChunkProvider<>(new ItemReader<String>() {
			@Nullable
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException {
				throw new RuntimeException("Planned");
			}
		}, new RepeatTemplate());
		provider.setSkipPolicy(new LimitCheckingItemSkipPolicy(Integer.MAX_VALUE, Collections.<Class<? extends Throwable>,Boolean>singletonMap(Exception.class, Boolean.TRUE)));
		provider.setMaxSkipsOnRead(10);
		Chunk<String> chunk = null;
		chunk = provider.provide(contribution);
		assertNotNull(chunk);
		assertEquals(0, chunk.getItems().size());
		assertEquals(10, chunk.getErrors().size());
	}
}
