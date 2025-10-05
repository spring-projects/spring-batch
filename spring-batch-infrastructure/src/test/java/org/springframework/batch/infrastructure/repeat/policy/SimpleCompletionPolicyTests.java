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

package org.springframework.batch.infrastructure.repeat.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCompletionPolicyTests {

	private final SimpleCompletionPolicy policy = new SimpleCompletionPolicy();

	private RepeatContext context;

	private final RepeatStatus dummy = RepeatStatus.CONTINUABLE;

	@BeforeEach
	void setUp() {
		context = policy.start(null);
	}

	@Test
	void testTerminationAfterDefaultSize() {
		for (int i = 0; i < SimpleCompletionPolicy.DEFAULT_CHUNK_SIZE - 1; i++) {
			policy.update(context);
			assertFalse(policy.isComplete(context, dummy));
		}
		policy.update(context);
		assertTrue(policy.isComplete(context, dummy));
	}

	@Test
	void testTerminationAfterExplicitChunkSize() {
		int chunkSize = 2;
		policy.setChunkSize(chunkSize);
		for (int i = 0; i < chunkSize - 1; i++) {
			policy.update(context);
			assertFalse(policy.isComplete(context, dummy));
		}
		policy.update(context);
		assertTrue(policy.isComplete(context, dummy));
	}

	@Test
	void testTerminationAfterNullResult() {
		policy.update(context);
		assertFalse(policy.isComplete(context, dummy));
		policy.update(context);
		assertTrue(policy.isComplete(context, null));
	}

	@Test
	void testReset() {
		policy.setChunkSize(2);
		policy.update(context);
		assertFalse(policy.isComplete(context, dummy));
		policy.update(context);
		assertTrue(policy.isComplete(context, dummy));
		context = policy.start(null);
		policy.update(context);
		assertFalse(policy.isComplete(context, dummy));
	}

}
