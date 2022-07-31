/*
 * Copyright 2006-2022 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * @author Dave Syer
 *
 */
class ChunkOrientedTaskletTests {

	private final ChunkContext context = new ChunkContext(null);

	@Test
	void testHandle() throws Exception {
		ChunkOrientedTasklet<String> handler = new ChunkOrientedTasklet<>(new ChunkProvider<String>() {
			@Override
			public Chunk<String> provide(StepContribution contribution) throws Exception {
				contribution.incrementReadCount();
				Chunk<String> chunk = new Chunk<>();
				chunk.add("foo");
				return chunk;
			}

			@Override
			public void postProcess(StepContribution contribution, Chunk<String> chunk) {
			}
		}, new ChunkProcessor<String>() {
			@Override
			public void process(StepContribution contribution, Chunk<String> chunk) {
				contribution.incrementWriteCount(1);
			}
		});
		StepContribution contribution = new StepContribution(
				new StepExecution("foo", new JobExecution(new JobInstance(123L, "job"), new JobParameters())));
		handler.execute(contribution, context);
		assertEquals(1, contribution.getReadCount());
		assertEquals(1, contribution.getWriteCount());
		assertEquals(0, context.attributeNames().length);
	}

	@Test
	void testFail() {
		ChunkOrientedTasklet<String> handler = new ChunkOrientedTasklet<>(new ChunkProvider<String>() {
			@Override
			public Chunk<String> provide(StepContribution contribution) throws Exception {
				throw new RuntimeException("Foo!");
			}

			@Override
			public void postProcess(StepContribution contribution, Chunk<String> chunk) {
			}
		}, new ChunkProcessor<String>() {
			@Override
			public void process(StepContribution contribution, Chunk<String> chunk) {
				fail("Not expecting to get this far");
			}
		});
		StepContribution contribution = new StepContribution(
				new StepExecution("foo", new JobExecution(new JobInstance(123L, "job"), new JobParameters())));
		Exception exception = assertThrows(RuntimeException.class, () -> handler.execute(contribution, context));
		assertEquals("Foo!", exception.getMessage());
		assertEquals(0, contribution.getReadCount());
	}

	@Test
	void testExitCode() throws Exception {
		ChunkOrientedTasklet<String> handler = new ChunkOrientedTasklet<>(new ChunkProvider<String>() {
			@Override
			public Chunk<String> provide(StepContribution contribution) throws Exception {
				contribution.incrementReadCount();
				Chunk<String> chunk = new Chunk<>();
				chunk.add("foo");
				chunk.setEnd();
				return chunk;
			}

			@Override
			public void postProcess(StepContribution contribution, Chunk<String> chunk) {
			}
		}, new ChunkProcessor<String>() {
			@Override
			public void process(StepContribution contribution, Chunk<String> chunk) {
				contribution.incrementWriteCount(1);
			}
		});
		StepContribution contribution = new StepContribution(
				new StepExecution("foo", new JobExecution(new JobInstance(123L, "job"), new JobParameters())));
		ExitStatus expected = contribution.getExitStatus();
		handler.execute(contribution, context);
		// The tasklet does not change the exit code
		assertEquals(expected, contribution.getExitStatus());
	}

}
