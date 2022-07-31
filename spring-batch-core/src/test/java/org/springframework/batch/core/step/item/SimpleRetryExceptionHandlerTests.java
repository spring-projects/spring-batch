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
package org.springframework.batch.core.step.item;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dave Syer
 *
 */
class SimpleRetryExceptionHandlerTests {

	private final RepeatContext context = new RepeatContextSupport(new RepeatContextSupport(null));

	@BeforeEach
	void setUp() {
		RepeatSynchronizationManager.register(context);
	}

	@AfterEach
	void tearDown() {
		RepeatSynchronizationManager.clear();
	}

	@Test
	void testRethrowWhenRetryExhausted() {

		RetryPolicy retryPolicy = new NeverRetryPolicy();
		RuntimeException ex = new RuntimeException("foo");

		SimpleRetryExceptionHandler handler = getHandlerAfterRetry(retryPolicy, ex, Set.of(Error.class));

		// Then pretend to handle the exception in the parent context...
		Exception exception = assertThrows(RuntimeException.class,
				() -> handler.handleException(context.getParent(), ex));
		assertEquals(ex, exception);

		assertEquals(0, context.attributeNames().length);
		// One for the retry exhausted flag and one for the counter in the
		// delegate exception handler
		assertEquals(2, context.getParent().attributeNames().length);
	}

	@Test
	void testNoRethrowWhenRetryNotExhausted() throws Throwable {

		RetryPolicy retryPolicy = new AlwaysRetryPolicy();
		RuntimeException ex = new RuntimeException("foo");

		SimpleRetryExceptionHandler handler = getHandlerAfterRetry(retryPolicy, ex,
				Collections.<Class<? extends Throwable>>singleton(Error.class));

		// Then pretend to handle the exception in the parent context...
		handler.handleException(context.getParent(), ex);

		assertEquals(0, context.attributeNames().length);
		assertEquals(0, context.getParent().attributeNames().length);
	}

	@Test
	void testRethrowWhenFatal() {

		RetryPolicy retryPolicy = new AlwaysRetryPolicy();
		RuntimeException ex = new RuntimeException("foo");

		SimpleRetryExceptionHandler handler = getHandlerAfterRetry(retryPolicy, ex,
				Collections.<Class<? extends Throwable>>singleton(RuntimeException.class));

		// Then pretend to handle the exception in the parent context...
		Exception exception = assertThrows(RuntimeException.class,
				() -> handler.handleException(context.getParent(), ex));
		assertEquals(ex, exception);

		assertEquals(0, context.attributeNames().length);
		// One for the counter in the delegate exception handler
		assertEquals(1, context.getParent().attributeNames().length);
	}

	private SimpleRetryExceptionHandler getHandlerAfterRetry(RetryPolicy retryPolicy, RuntimeException ex,
			Collection<Class<? extends Throwable>> fatalExceptions) {

		// Always rethrow if the retry is exhausted
		SimpleRetryExceptionHandler handler = new SimpleRetryExceptionHandler(retryPolicy,
				new SimpleLimitExceptionHandler(0), fatalExceptions);

		// Simulate a failed retry...
		RetryContext retryContext = retryPolicy.open(null);
		retryPolicy.registerThrowable(retryContext, ex);
		handler.close(retryContext, null, ex);
		return handler;
	}

}
