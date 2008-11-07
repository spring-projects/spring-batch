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
import org.springframework.batch.repeat.context.RepeatContextSupport;

public class CountingCompletionPolicyTests extends TestCase {

	public void testDefaultBehaviour() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			protected int getCount(RepeatContext context) {
				return 1;
			};
		};
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context));
	}

	public void testNullResult() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			protected int getCount(RepeatContext context) {
				return 1;
			};
		};
		policy.setMaxCount(10);
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context, null));
	}

	public void testFinishedResult() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			protected int getCount(RepeatContext context) {
				return 1;
			};
		};
		policy.setMaxCount(10);
		RepeatContext context = policy.start(null);
		assertTrue(policy.isComplete(context, RepeatStatus.FINISHED));
	}

	public void testDefaultBehaviourWithUpdate() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			protected int getCount(RepeatContext context) {
				return count;
			};

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

	public void testUpdateNotSavedAcrossSession() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			protected int getCount(RepeatContext context) {
				return count;
			};

			protected int doUpdate(RepeatContext context) {
				super.doUpdate(context);
				count++;
				return 1;
			}

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

	public void testUpdateSavedAcrossSession() throws Exception {
		CountingCompletionPolicy policy = new CountingCompletionPolicy() {
			int count = 0;

			protected int getCount(RepeatContext context) {
				return count;
			};

			protected int doUpdate(RepeatContext context) {
				super.doUpdate(context);
				count++;
				return 1;
			}

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
