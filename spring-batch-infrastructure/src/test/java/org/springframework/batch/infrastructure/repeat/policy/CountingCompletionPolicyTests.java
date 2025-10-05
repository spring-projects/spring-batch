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
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.context.RepeatContextSupport;
import org.springframework.batch.infrastructure.repeat.policy.CountingCompletionPolicy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountingCompletionPolicyTests {

	@Test
	void testDefaultBehaviour() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			@Override
			protected int getCount(RepeatContext context) {
				return 1;
			}
		};
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context));
	}

	@Test
	void testNullResult() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			@Override
			protected int getCount(RepeatContext context) {
				return 1;
			}
		};
		policy.setMaxCount(10);
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context, null));
	}

	@Test
	void testFinishedResult() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			@Override
			protected int getCount(RepeatContext context) {
				return 1;
			}
		};
		policy.setMaxCount(10);
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context, RepeatStatus.FINISHED));
	}

	@Test
	void testDefaultBehaviourWithUpdate() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			@Override
			protected int getCount(RepeatContext context) {
				return count;
			}

			@Override
			protected int doUpdate(RepeatContext context) {
				count++;
				return 1;
			}
		};
		policy.setMaxCount(2);
		RepeatContext context = policy.start(null);
		policy.update(context);
		assertFalse(policy.isComplete(context));
		policy.update(context);
		assertTrue(policy.isComplete(context));
	}

	@Test
	void testUpdateNotSavedAcrossSession() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			@Override
			protected int getCount(RepeatContext context) {
				return count;
			}

			@Override
			protected int doUpdate(RepeatContext context) {
				super.doUpdate(context);
				count++;
				return 1;
			}

			@Override
			public RepeatContext start(RepeatContext context) {
				count = 0;
				return super.start(context);
			}
		};
		policy.setMaxCount(2);
		RepeatContextSupport session = new RepeatContextSupport(null);
		RepeatContext context = policy.start(session);
		policy.update(context);
		assertFalse(policy.isComplete(context));
		context = policy.start(session);
		policy.update(context);
		assertFalse(policy.isComplete(context));
	}

	@Test
	void testUpdateSavedAcrossSession() {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			@Override
			protected int getCount(RepeatContext context) {
				return count;
			}

			@Override
			protected int doUpdate(RepeatContext context) {
				super.doUpdate(context);
				count++;
				return 1;
			}

			@Override
			public RepeatContext start(RepeatContext context) {
				count = 0;
				return super.start(context);
			}
		};
		policy.setMaxCount(2);
		policy.setUseParent(true);
		RepeatContextSupport session = new RepeatContextSupport(null);
		RepeatContext context = policy.start(session);
		policy.update(context);
		assertFalse(policy.isComplete(context));
		context = policy.start(session);
		policy.update(context);
		assertTrue(policy.isComplete(context));
	}

}
