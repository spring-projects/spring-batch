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

package org.springframework.batch.repeat.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;

public class RethrowOnThresholdExceptionHandlerTests {

	private RethrowOnThresholdExceptionHandler handler = new RethrowOnThresholdExceptionHandler();

	private RepeatContext parent = new RepeatContextSupport(null);

	private RepeatContext context = new RepeatContextSupport(parent);

	@Test
	public void testRuntimeException() throws Throwable {
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	@Test
	public void testError() throws Throwable {
		try {
			handler.handleException(context, new Error("Foo"));
			fail("Expected Error");
		}
		catch (Error e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	@Test
	public void testNotRethrownWithThreshold() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer> singletonMap(Exception.class, 1));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		AtomicInteger counter = (AtomicInteger) context.getAttribute(context.attributeNames()[0]);
		assertNotNull(counter);
		assertEquals(1, counter.get());
	}

	@Test
	public void testRethrowOnThreshold() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer> singletonMap(Exception.class, 2));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		handler.handleException(context, new RuntimeException("Foo"));
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	@Test
	public void testNotUseParent() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer> singletonMap(Exception.class, 1));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		context = new RepeatContextSupport(parent);
		try {
			// No exception again - context is changed...
			handler.handleException(context, new RuntimeException("Foo"));
		}
		catch (RuntimeException e) {
			fail("Unexpected Error");
		}
	}

	@Test
	public void testUseParent() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer> singletonMap(Exception.class, 1));
		handler.setUseParent(true);
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		context = new RepeatContextSupport(parent);
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected Error");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

}
