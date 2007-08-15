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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.exception.TerminatedRetryException;
import org.springframework.batch.retry.synch.RetrySynchronizationManager;

public class CompositeRetryPolicyTests extends TestCase {

	public void testEmptyPolicies() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		RetryContext context = policy.open(null);
		assertNotNull(context);
		assertTrue(policy.canRetry(context));
	}

	public void testTrivialPolicies() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
		RetryContext context = policy.open(null);
		assertNotNull(context);
		assertTrue(policy.canRetry(context));
	}

	public void testNonTrivialPolicies() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() {
			public boolean canRetry(RetryContext context) {
				return false;
			}
		} });
		RetryContext context = policy.open(null);
		assertNotNull(context);
		assertFalse(policy.canRetry(context));
	}

	public void testNonTrivialPoliciesWithThrowable() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() {
			boolean errorRegistered = false;

			public boolean canRetry(RetryContext context) {
				return !errorRegistered;
			}

			public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
				errorRegistered = true;
			}
		} });
		RetryContext context = policy.open(null);
		assertNotNull(context);
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, null);
		assertFalse(policy.canRetry(context));
	}

	public void testNonTrivialPoliciesClose() throws Exception {
		final List list = new ArrayList();
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport() {
			public void close(RetryContext context) {
				list.add("1");
				// TODO: test that all close methods are called if this
				// happens...
				// throw new RuntimeException("Pah!");
			}
		}, new MockRetryPolicySupport() {
			public void close(RetryContext context) {
				list.add("2");
			}
		} });
		RetryContext context = policy.open(null);
		assertNotNull(context);
		policy.close(context);
		assertEquals(2, list.size());
	}

	public void testRetryCount() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
		RetryContext context = policy.open(null);
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	public void testParent() throws Exception {
		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		RetryContext context = policy.open(null);
		RetrySynchronizationManager.register(context);
		RetryContext child = policy.open(null);
		assertNotSame(child, context);
		assertSame(context, child.getParent());
	}

}
