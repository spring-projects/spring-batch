/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.repeat.policy;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;

public class SimpleCompletionPolicyTests extends TestCase {

	SimpleCompletionPolicy policy = new SimpleCompletionPolicy();

	RepeatContext context;

	RepeatStatus dummy = RepeatStatus.CONTINUABLE;

	protected void setUp() throws Exception {
		super.setUp();
		context = policy.start(null);
	}

	public void testTerminationAfterDefaultSize() throws Exception {
		for (int i = 0; i < SimpleCompletionPolicy.DEFAULT_CHUNK_SIZE - 1; i++) {
			policy.update(context);
			assertFalse(policy.isComplete(context, dummy));
		}
		policy.update(context);
		assertTrue(policy.isComplete(context, dummy));
	}

	public void testTerminationAfterExplicitChunkSize() throws Exception {
		int chunkSize = 2;
		policy.setChunkSize(chunkSize);
		for (int i = 0; i < chunkSize - 1; i++) {
			policy.update(context);
			assertFalse(policy.isComplete(context, dummy));
		}
		policy.update(context);
		assertTrue(policy.isComplete(context, dummy));
	}

	public void testTerminationAfterNullResult() throws Exception {
		policy.update(context);
		assertFalse(policy.isComplete(context, dummy));
		policy.update(context);
		assertTrue(policy.isComplete(context, null));
	}

	public void testReset() throws Exception {
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
