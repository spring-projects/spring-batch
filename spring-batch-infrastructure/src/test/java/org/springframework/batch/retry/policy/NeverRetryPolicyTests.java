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

package org.springframework.batch.retry.policy;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryContext;

public class NeverRetryPolicyTests extends TestCase {

	public void testSimpleOperations() throws Exception {
		NeverRetryPolicy policy = new NeverRetryPolicy();
		RetryContext context = policy.open(null);
		assertNotNull(context);
		// We can retry until the first exception is registered...
		assertTrue(policy.canRetry(context));
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, null);
		assertFalse(policy.canRetry(context));
		policy.close(context);
		assertFalse(policy.canRetry(context));
	}

	public void testRetryCount() throws Exception {
		NeverRetryPolicy policy = new NeverRetryPolicy();
		RetryContext context = policy.open(null);
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	public void testParent() throws Exception {
		NeverRetryPolicy policy = new NeverRetryPolicy();
		RetryContext context = policy.open(null);
		RetryContext child = policy.open(context);
		assertNotSame(child, context);
		assertSame(context, child.getParent());
	}

}
