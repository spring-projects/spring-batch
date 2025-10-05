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

package org.springframework.batch.infrastructure.repeat.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.context.RepeatContextSupport;
import org.springframework.batch.infrastructure.repeat.exception.RethrowOnThresholdExceptionHandler;

class RethrowOnThresholdExceptionHandlerTests {

	private final RethrowOnThresholdExceptionHandler handler = new RethrowOnThresholdExceptionHandler();

	private final RepeatContext parent = new RepeatContextSupport(null);

	private RepeatContext context = new RepeatContextSupport(parent);

	@Test
	void testRuntimeException() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> handler.handleException(context, new RuntimeException("Foo")));
		assertEquals("Foo", exception.getMessage());
	}

	@Test
	void testError() {
		Error error = assertThrows(Error.class, () -> handler.handleException(context, new Error("Foo")));
		assertEquals("Foo", error.getMessage());
	}

	@Test
	void testNotRethrownWithThreshold() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer>singletonMap(Exception.class, 1));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		AtomicInteger counter = (AtomicInteger) context.getAttribute(context.attributeNames()[0]);
		assertNotNull(counter);
		assertEquals(1, counter.get());
	}

	@Test
	void testRethrowOnThreshold() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer>singletonMap(Exception.class, 2));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		handler.handleException(context, new RuntimeException("Foo"));
		Exception exception = assertThrows(RuntimeException.class,
				() -> handler.handleException(context, new RuntimeException("Foo")));
		assertEquals("Foo", exception.getMessage());
	}

	@Test
	void testNotUseParent() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer>singletonMap(Exception.class, 1));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		context = new RepeatContextSupport(parent);
		// No exception again - context is changed...
		assertDoesNotThrow(() -> handler.handleException(context, new RuntimeException("Foo")));
	}

	@Test
	void testUseParent() throws Throwable {
		handler.setThresholds(Collections.<Class<? extends Throwable>, Integer>singletonMap(Exception.class, 1));
		handler.setUseParent(true);
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		context = new RepeatContextSupport(parent);
		Exception exception = assertThrows(RuntimeException.class,
				() -> handler.handleException(context, new RuntimeException("Foo")));
		assertEquals("Foo", exception.getMessage());
	}

}
