/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.repeat.support;

import org.junit.Test;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.callback.NestedRepeatCallback;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test various approaches to chunking of a batch. Not really a unit test, but
 * it should be fast.
 * 
 * @author Dave Syer
 * 
 */
public class ChunkedRepeatTests extends AbstractTradeBatchTests {

	int count = 0;

	/**
	 * Chunking using a dedicated TerminationPolicy. Transactions would be laid
	 * on at the level of chunkTemplate.execute() or the surrounding callback.
	 */
	@Test
	public void testChunkedBatchWithTerminationPolicy() throws Exception {

		RepeatTemplate repeatTemplate = new RepeatTemplate();
		final RepeatCallback callback = new ItemReaderRepeatCallback<>(provider, processor);

		final RepeatTemplate chunkTemplate = new RepeatTemplate();
		// The policy is resettable so we only have to resolve this dependency
		// once
		chunkTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));

		RepeatStatus result = repeatTemplate.iterate(new NestedRepeatCallback(chunkTemplate, callback) {

            @Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++; // for test assertion
				return super.doInIteration(context);
			}

		});

		assertEquals(NUMBER_OF_ITEMS, processor.count);
		// The chunk executes 3 times because the last one
		// returns false. We terminate the main batch when
		// we encounter a partially empty chunk.
		assertEquals(3, count);
		assertFalse(result.isContinuable());

	}

	/**
	 * Chunking with an asynchronous taskExecutor in the chunks. Transactions
	 * have to be at the level of the business callback.
	 */
	@Test
	public void testAsynchronousChunkedBatchWithCompletionPolicy() throws Exception {

		RepeatTemplate repeatTemplate = new RepeatTemplate();
		final RepeatCallback callback = new ItemReaderRepeatCallback<>(provider, processor);

		final TaskExecutorRepeatTemplate chunkTemplate = new TaskExecutorRepeatTemplate();
		// The policy is resettable so we only have to resolve this dependency
		// once
		chunkTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
		chunkTemplate.setTaskExecutor(new SimpleAsyncTaskExecutor());

		RepeatStatus result = repeatTemplate.iterate(new NestedRepeatCallback(chunkTemplate, callback) {

            @Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++; // for test assertion
				return super.doInIteration(context);
			}

		});

		assertEquals(NUMBER_OF_ITEMS, processor.count);
		assertFalse(result.isContinuable());
		assertTrue("Expected at least 3 chunks but found: "+count, count>=3);

	}

	/**
	 * Explicit chunking of input data. Transactions would be laid on at the
	 * level of template.execute().
	 */
	@Test
	public void testChunksWithTruncatedItemProvider() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// This pattern would work with an asynchronous callback as well
		// (but non-transactional in that case).

		class Chunker {
			boolean ready = false;

			int count = 0;

			void set() {
				ready = true;
			}

			boolean ready() {
				return ready;
			}

			boolean first() {
				return count == 0;
			}

			void reset() {
				count = 0;
				ready = false;
			}

			void increment() {
				count++;
			}
		}

		final Chunker chunker = new Chunker();

		while (!chunker.ready()) {

			ItemReader<Trade> truncated = new ItemReader<Trade>() {
				int count = 0;

                @Nullable
				@Override
				public Trade read() throws Exception {
					if (count++ < 2)
						return provider.read();
					return null;
				}
			};
			chunker.reset();
			template.iterate(new ItemReaderRepeatCallback<Trade>(truncated, processor) {

                @Override
				public RepeatStatus doInIteration(RepeatContext context) throws Exception {
					RepeatStatus result = super.doInIteration(context);
					if (!result.isContinuable() && chunker.first()) {
						chunker.set();
					}
					chunker.increment();
					return result;
				}

			});

		}

		assertEquals(NUMBER_OF_ITEMS, processor.count);

	}

}
