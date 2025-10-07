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

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.CompletionPolicy;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeCompletionPolicyTests {

	@Test
	void testEmptyPolicies() {
		CompositeCompletionPolicy policy = new CompositeCompletionPolicy();
		RepeatContext context = policy.start(null);
		assertNotNull(context);
		assertFalse(policy.isComplete(context));
	}

	@Test
	void testTrivialPolicies() {
		CompositeCompletionPolicy policy = new CompositeCompletionPolicy();
		policy.setPolicies(
				new CompletionPolicy[] { new MockCompletionPolicySupport(), new MockCompletionPolicySupport() });
		RepeatContext context = policy.start(null);
		assertEquals(0, context.getStartedCount());
		assertFalse(policy.isComplete(context));
		assertFalse(policy.isComplete(context, null));
		policy.update(context);
		assertEquals(1, context.getStartedCount());
	}

	@Test
	void testNonTrivialPolicies() {
		CompositeCompletionPolicy policy = new CompositeCompletionPolicy();
		policy
			.setPolicies(new CompletionPolicy[] { new MockCompletionPolicySupport(), new MockCompletionPolicySupport() {
				@Override
				public boolean isComplete(RepeatContext context) {
					return true;
				}
			} });
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context));
	}

	@Test
	void testNonTrivialPoliciesWithResult() {
		CompositeCompletionPolicy policy = new CompositeCompletionPolicy();
		policy
			.setPolicies(new CompletionPolicy[] { new MockCompletionPolicySupport(), new MockCompletionPolicySupport() {
				@Override
				public boolean isComplete(RepeatContext context, RepeatStatus result) {
					return true;
				}
			} });
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context, null));
	}

}
