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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.policy.TimeoutTerminationPolicy;

class TimeoutCompletionPolicyTests {

	@Test
	void testSimpleTimeout() throws Exception {
		TimeoutTerminationPolicy policy = new TimeoutTerminationPolicy(20L);
		RepeatContext context = policy.start(null);
		assertFalse(policy.isComplete(context));
		Thread.sleep(50L);
		assertTrue(policy.isComplete(context));
	}

	@Test
	void testSuccessfulResult() {
		TimeoutTerminationPolicy policy = new TimeoutTerminationPolicy();
		RepeatContext context = policy.start(null);
		assertFalse(policy.isComplete(context, null));
	}

	@Test
	void testNonContinuableResult() {
		TimeoutTerminationPolicy policy = new TimeoutTerminationPolicy();
		RepeatStatus result = RepeatStatus.FINISHED;
		assertTrue(policy.isComplete(policy.start(null), result));
	}

	@Test
	void testUpdate() throws Exception {
		TimeoutTerminationPolicy policy = new TimeoutTerminationPolicy(20L);
		RepeatContext context = policy.start(null);
		assertFalse(policy.isComplete(context));
		Thread.sleep(50L);
		assertTrue(policy.isComplete(context));
		policy.update(context);
		// update doesn't change completeness
		assertTrue(policy.isComplete(context));
	}

}
