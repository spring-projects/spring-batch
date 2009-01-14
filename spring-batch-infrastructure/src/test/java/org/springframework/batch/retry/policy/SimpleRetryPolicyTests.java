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

import static org.junit.Assert.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;


import org.junit.Test;
import org.springframework.batch.retry.RetryContext;

public class SimpleRetryPolicyTests {

	@Test
	public void testCanRetryIfNoException() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);
		assertTrue(policy.canRetry(context));
	}

	@Test
	public void testEmptyExceptionsNeverRetry() throws Exception {

		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);

		// We can't retry any exceptions...
		Collection<Class<? extends Throwable>> empty = Collections.emptySet();
		policy.setRetryableExceptionClasses(empty);

		// ...so we can't retry this one...
		policy.registerThrowable(context, new IllegalStateException());
		assertFalse(policy.canRetry(context));
	}

	@Test
	public void testRetryLimitInitialState() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);
		assertTrue(policy.canRetry(context));
		policy.setMaxAttempts(0);
		context = policy.open(null);
		assertFalse(policy.canRetry(context));
	}

	@Test
	public void testRetryLimitSubsequentState() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);
		policy.setMaxAttempts(2);
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
	}

	@Test
	public void testRetryCount() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	@Test
	public void testFatalOverridesRetryable() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		policy.setFatalExceptionClasses(getClasses(Exception.class));
		policy.setRetryableExceptionClasses(getClasses(RuntimeException.class));
		RetryContext context = policy.open(null);
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

	@Test
	public void testParent() throws Exception {
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		RetryContext context = policy.open(null);
		RetryContext child = policy.open(context);
		assertNotSame(child, context);
		assertSame(context, child.getParent());
	}

}
