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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;

/**
 * Unit tests for {@link SimpleLimitExceptionHandler}
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class SimpleLimitExceptionHandlerTests extends TestCase {

	// object under test
	private SimpleLimitExceptionHandler handler = new SimpleLimitExceptionHandler();

	public void testInitializeWithNullContext() throws Throwable {
		try {
			handler.handleException(null, new RuntimeException("foo"));
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testInitializeWithNullContextAndNullException() throws Throwable {
		try {
			handler.handleException(null, null);
		} catch (NullPointerException e) {
			// expected;
		}
	}

	/**
	 * Other than nominated exception type should be rethrown, ignoring the exception limit.
	 * 
	 * @throws Exception
	 */
	public void testNormalExceptionThrown() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(new Class[] { IllegalArgumentException.class });

		try {
			handler.handleException(new RepeatContextSupport(null), throwable);
			fail("Exception swallowed.");
		} catch (RuntimeException expected) {
			assertTrue("Exception is rethrown, ignoring the exception limit", true);
			assertSame(expected, throwable);
		}
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * 
	 * @throws Exception
	 */
	public void testLimitedExceptionTypeNotThrown() throws Throwable {
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(new Class[] {RuntimeException.class} );

		try {
			handler.handleException(new RepeatContextSupport(null), new RuntimeException("foo"));
		} catch (RuntimeException expected) {
			fail("Unexpected exception.");
		}
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * 
	 * @throws Exception
	 */
	public void testLimitedExceptionNotThrownFromSiblings() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(new Class[] {RuntimeException.class});

		RepeatContextSupport parent = new RepeatContextSupport(null);

		try {
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
			context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
		} catch (RuntimeException expected) {
			fail("Unexpected exception.");
		}
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 * 
	 * @throws Exception
	 */
	public void testLimitedExceptionThrownFromSiblingsWhenUsingParent() throws Throwable {
		Throwable throwable = new RuntimeException("foo");

		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setExceptionClasses(new Class[] { RuntimeException.class } );
		handler.setUseParent(true);

		RepeatContextSupport parent = new RepeatContextSupport(null);

		try {
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
			context = new RepeatContextSupport(parent);
			handler.handleException(context, throwable);
			fail("Expected exception.");
		} catch (RuntimeException expected) {
			assertSame(throwable, expected);
		}
	}

	/**
	 * TransactionInvalidExceptions are swallowed until the exception limit is exceeded. After the limit is exceeded
	 * exceptions are rethrown as BatchCriticalExceptions
	 */
	public void testExceptionNotThrownBelowLimit() throws Throwable {

		final int EXCEPTION_LIMIT = 3;
		handler.setLimit(EXCEPTION_LIMIT);

		List throwables = new ArrayList() {
			{
				for (int i = 0; i < (EXCEPTION_LIMIT); i++) {
					add(new RuntimeException("below exception limit"));
				}
			}
		};

		RepeatContextSupport context = new RepeatContextSupport(null);

		try {
			for (Iterator iterator = throwables.iterator(); iterator.hasNext();) {
				Throwable throwable = (Throwable) iterator.next();

				handler.handleException(context, throwable);
				assertTrue("exceptions up to limit are swallowed", true);

			}
		} catch (RuntimeException unexpected) {
			fail("exception rethrown although exception limit was not exceeded");
		}

	}

	/**
	 * TransactionInvalidExceptions are swallowed until the exception limit is exceeded. After the limit is exceeded
	 * exceptions are rethrown as BatchCriticalExceptions
	 */
	public void testExceptionThrownAboveLimit() throws Throwable {

		final int EXCEPTION_LIMIT = 3;
		handler.setLimit(EXCEPTION_LIMIT);

		List throwables = new ArrayList() {
			{
				for (int i = 0; i < (EXCEPTION_LIMIT); i++) {
					add(new RuntimeException("below exception limit"));
				}
			}
		};

		throwables.add(new RuntimeException("above exception limit"));

		RepeatContextSupport context = new RepeatContextSupport(null);

		try {
			for (Iterator iterator = throwables.iterator(); iterator.hasNext();) {
				Throwable throwable = (Throwable) iterator.next();

				handler.handleException(context, throwable);
				assertTrue("exceptions up to limit are swallowed", true);

			}
		} catch (RuntimeException expected) {
			assertEquals("above exception limit", expected.getMessage());
		}

		// after reaching the limit, behaviour should be idempotent
		try {
			handler.handleException(context, new RuntimeException("foo"));
			assertTrue("exceptions up to limit are swallowed", true);

		} catch (RuntimeException expected) {
			assertEquals("foo", expected.getMessage());
		}
	}
}
