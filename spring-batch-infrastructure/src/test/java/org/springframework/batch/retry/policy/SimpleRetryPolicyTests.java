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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryContext;

public class SimpleRetryPolicyTests extends TestCase {

	public void testCanRetryIfNoException() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		assertTrue(policy.canRetry(context));
	}

	@SuppressWarnings("unchecked")
	public void testEmptyExceptionsNeverRetry() throws Exception {

		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);

		// We can't retry any exceptions...
		policy.setRetryableExceptionClasses(Collections.EMPTY_SET);

		// ...so we can't retry this one...
		policy.registerThrowable(context, new IllegalStateException());
		assertFalse(policy.canRetry(context));
	}

	public void testRetryLimitInitialState() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		assertTrue(policy.canRetry(context));
		policy.setMaxAttempts(0);
		context = policy.open(null, null);
		assertFalse(policy.canRetry(context));
	}

	public void testRetryLimitSubsequentState() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		policy.setMaxAttempts(2);
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
	}

	public void testRetryCount() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	public void testDefaultFatal() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		assertNotNull(context);
		policy.registerThrowable(context, new Error("foo"));
		assertFalse(policy.canRetry(context));
	}

	public void testFatalOverridesRetryable() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		policy.setFatalExceptionClasses(getClasses(Exception.class));
		policy.setRetryableExceptionClasses(getClasses(RuntimeException.class));
		RetryContext context = policy.open(null, null);
		assertNotNull(context);
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertFalse(policy.canRetry(context));
	}

	/**
	 * @param cls
	 * @return
	 */
	private Collection<Class<? extends Throwable>> getClasses(Class<? extends Throwable> cls) {
		Collection<Class<? extends Throwable>> classes = new HashSet<Class<? extends Throwable>>();
		classes.add(cls);
		return classes;
	}

	public void testParent() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null, null);
		RetryContext child = policy.open(null, context);
		assertNotSame(child, context);
		assertSame(context, child.getParent());
	}

}
