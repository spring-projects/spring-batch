/*
 * Copyright 2024 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tests for concurrent behaviour in repeat template, dedicated to the first chunk, that
 * must be managed first when output format has separator between items, like JSON.
 *
 * @author Gerald Lelarge
 *
 */
class TaskExecutorRepeatTemplateFirstChunkTests extends AbstractTradeBatchTests {

	private TaskExecutorRepeatTemplate template;

	private int chunkSize = 5;

	private final ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();

	@BeforeEach
	void setUp() throws Exception {

		super.setUp();

		threadPool.setMaxPoolSize(10);
		threadPool.setCorePoolSize(10);
		threadPool.setQueueCapacity(0);
		threadPool.afterPropertiesSet();

		template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(threadPool);
		// Limit the number of threads to 2
		template.setThrottleLimit(2);
		// Limit the number of items to read to be able to test the second item from the
		// output. If the chunkSize is greater than 2, the test could fail.
		template.setCompletionPolicy(new SimpleCompletionPolicy(chunkSize));
	}

	@AfterEach
	void tearDown() {
		threadPool.destroy();
	}

	/**
	 * Test method for {@link TaskExecutorRepeatTemplate#iterate(RepeatCallback)}. Repeat
	 * the tests 20 times to increase the probability of detecting a concurrency.
	 */
	@Test
	@RepeatedTest(value = 20)
	void testExecute() {

		// given
		template.iterate(new ItemReaderRepeatCallback<>(provider, processor));

		// then
		// The first element is the first item of the input trades.csv.
		assertEquals("UK21341EAH45", output.get(0).getIsin());
		// The others can have different orders.
		for (int i = 1; i < output.size(); i++) {
			assertNotEquals("UK21341EAH45", output.get(i).getIsin());
		}
		assertEquals(chunkSize, processor.count);
	}

}
