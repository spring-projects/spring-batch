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

package org.springframework.batch.repeat.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * Unit tests for {@link SimpleLimitExceptionHandler}
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 */
class SimpleLimitExceptionHandlerTests {

	// object under test
	private final SimpleLimitExceptionHandler handler = new SimpleLimitExceptionHandler();

	@BeforeEach
	void initializeHandler() throws Exception {
		handler.afterPropertiesSet();
	}

	@Test
	void testInitializeWithNullContext() {
		assertThrows(IllegalArgumentException.class, () -> handler.handleException(null, new RuntimeException("foo")));
	}

	@Test
	void testInitializeWithNullContextAndNullException() {
		assertThrows(IllegalArgumentException.class, () -> handler.handleException(null, null));
	}

	@Test
	void testDefaultBehaviour() {
		Throwable throwable = new RuntimeException("foo");
		Exception expected = assertThrows(RuntimeException.class,
				() -> handler.handleException(new RepeatContextSupport(null), throwable));
		assertSame(expected, throwable);
	}

	/**
	 * Other than nominated exception type should be rethrown, ignoring the exception
	 * limit.
	 * @throws Exception
	 */
	@Test
	void testNormalExceptionThrown() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(Collections.<Class<? extends Throwable>>singleton(IllegalArgumentException.class));
		handler.afterPropertiesSet();

		Exception expected = assertThrows(RuntimeException.class,
				() -> handler.handleException(new RepeatContextSupport(null), throwable));
		assertSame(expected, throwable);
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * @throws Exception
	 */
	@Test
	void testLimitedExceptionTypeNotThrown() throws Throwable {
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(Collections.<Class<? extends Throwable>>singleton(RuntimeException.class));
		handler.afterPropertiesSet();

		assertDoesNotThrow(() -> handler.handleException(new RepeatContextSupport(null), new RuntimeException("foo")));
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * @throws Exception
	 */
	@Test
	void testLimitedExceptionNotThrownFromSiblings() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(Collections.<Class<? extends Throwable>>singleton(RuntimeException.class));
		handler.afterPropertiesSet();

		RepeatContextSupport parent = new RepeatContextSupport(null);

		assertDoesNotThrow(() -> {
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
			context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
		});
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * @throws Exception
	 */
	@Test
	void testLimitedExceptionThrownFromSiblingsWhenUsingParent() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(Collections.<Class<? extends Throwable>>singleton(RuntimeException.class));
		handler.setUseParent(true);
		handler.afterPropertiesSet();

		RepeatContextSupport parent = new RepeatContextSupport(null);

		Exception expected = assertThrows(RuntimeException.class, () -> {
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
			context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
		});
		assertSame(throwable, expected);
	}

	/**
	 * Exceptions are swallowed until the exception limit is exceeded. After the limit is
	 * exceeded exceptions are rethrown
	 */
	@Test
	void testExceptionNotThrownBelowLimit() throws Throwable {

		final int EXCEPTION_LIMIT = 3;
		handler.setLimit(EXCEPTION_LIMIT);
		handler.afterPropertiesSet();

		@SuppressWarnings("serial")
		List<Throwable> throwables = new ArrayList<Throwable>() {
			{
				for (int i = 0; i < (EXCEPTION_LIMIT); i++) {
					add(new RuntimeException("below exception limit"));
				}
			}
		};

		RepeatContextSupport context = new RepeatContextSupport(null);

		for (Throwable throwable : throwables) {
			assertDoesNotThrow(() -> handler.handleException(context, throwable));
		}

	}

	/**
	 * TransactionInvalidExceptions are swallowed until the exception limit is exceeded.
	 * After the limit is exceeded exceptions are rethrown as BatchCriticalExceptions
	 */
	@Test
	void testExceptionThrownAboveLimit() throws Throwable {

		final int EXCEPTION_LIMIT = 3;
		handler.setLimit(EXCEPTION_LIMIT);
		handler.afterPropertiesSet();

		@SuppressWarnings("serial")
		List<Throwable> throwables = new ArrayList<Throwable>() {
			{
				for (int i = 0; i < (EXCEPTION_LIMIT); i++) {
					add(new RuntimeException("below exception limit"));
				}
			}
		};

		throwables.add(new RuntimeException("above exception limit"));

		RepeatContextSupport context = new RepeatContextSupport(null);

		Exception expected = assertThrows(RuntimeException.class, () -> {
			for (Throwable throwable : throwables) {
				handler.handleException(context, throwable);
			}
		});
		assertEquals("above exception limit", expected.getMessage());

		// after reaching the limit, behaviour should be idempotent
		expected = assertThrows(RuntimeException.class,
				() -> handler.handleException(context, new RuntimeException("foo")));
		assertEquals("foo", expected.getMessage());
	}

}
