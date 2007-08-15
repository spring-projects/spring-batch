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

package org.springframework.batch.repeat.exception.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.TransactionInvalidException;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.exception.handler.SimpleLimitExceptionHandler;

/**
 * Unit tests for {@link SimpleLimitExceptionHandler}
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class SimpleLimitExceptionHandlerTests extends TestCase {

	// object under test
	private SimpleLimitExceptionHandler handler = new SimpleLimitExceptionHandler();
	
	public void testInitializeWithNullContext() throws Exception {
		try {
			handler.handleExceptions(null, Collections.singleton(new RuntimeException("foo")));
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testInitializeWithNullContextAndEmptyList() throws Exception {
		try {
			handler.handleExceptions(null, Collections.EMPTY_LIST);
		} catch (Exception e) {
			fail("Unexpected IllegalArgumentException");
		}
	}

	/**
	 * Other than TransactionInvalidException should be rethrown, ignoring the exception limit.
	 */
	public void testNormalExceptionThrown() {
		List throwables = Collections.singletonList(new RuntimeException("foo"));
		
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		
		try{
			handler.handleExceptions(new RepeatContextSupport(null), throwables);
			fail("Exception swallowed.");
		} catch (RuntimeException expected) {
			assertTrue("Exception is rethrown, ignoring the exception limit",true);
			assertSame(throwables.get(0), expected);
		}
	}
	
	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 */
	public void testLimitedExceptionTypeNotThrown() {
		List throwables = Collections.singletonList(new RuntimeException("foo"));
		
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setType(RuntimeException.class);
		
		try{
			handler.handleExceptions(new RepeatContextSupport(null), throwables);
		} catch (RuntimeException expected) {
			fail("Unexpected exception.");
		}
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 */
	public void testLimitedExceptionNotThrownFromSiblings() {
		List throwables = Collections.singletonList(new RuntimeException("foo"));
		
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setType(RuntimeException.class);
		
		RepeatContextSupport parent = new RepeatContextSupport(null);

		try{
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleExceptions(context, throwables);
			context = new RepeatContextSupport(parent);
			handler.handleExceptions(context, throwables);
		} catch (RuntimeException expected) {
			fail("Unexpected exception.");
		}
	}

	/**
	 * TransactionInvalidException should only be rethrown below the exception limit.
	 */
	public void testLimitedExceptionThrownFromSiblingsWhenUsingParent() {
		List throwables = Collections.singletonList(new RuntimeException("foo"));
		
		final int MORE_THAN_ZERO = 1;
		handler.setLimit(MORE_THAN_ZERO);
		handler.setType(RuntimeException.class);
		handler.setUseParent(true);
		
		RepeatContextSupport parent = new RepeatContextSupport(null);

		try{
			RepeatContextSupport context = new RepeatContextSupport(parent);
			handler.handleExceptions(context, throwables);
			context = new RepeatContextSupport(parent);
			handler.handleExceptions(context, throwables);
			fail("Expected exception.");
		} catch (RuntimeException expected) {
			assertSame(throwables.get(0), expected);
		}
	}

	/**
	 * TransactionInvalidExceptions are swallowed until the exception limit is exceeded.
	 * After the limit is exceeded exceptions are rethrown as BatchCriticalExceptions
	 */
	public void testExceptionThrownAboveLimit() {
		
		final int EXCEPTION_LIMIT = 3;
		handler.setLimit(EXCEPTION_LIMIT);
		
		List throwables = new ArrayList() {{
			for (int i = 0; i < (EXCEPTION_LIMIT); i++) {
				add(new TransactionInvalidException("below exception limit"));
			}
		}};
		
		RepeatContextSupport context = new RepeatContextSupport(null);

		try {
			handler.handleExceptions(context, throwables);
			assertTrue("exceptions up to limit are swallowed", true);
		} catch (RuntimeException unexpected) {
			fail("exception rethrown although exception limit was not exceeded");
		}
		
		
		throwables = new ArrayList() {{
			add(new TransactionInvalidException("above exception limit"));
		}};
		
		// after reaching the limit, behaviour should be idempotent
		final int ARBITRARY_REPEAT_COUNT = 2;
		for (int i = 0; i < ARBITRARY_REPEAT_COUNT; i++) {		
			try {
				handler.handleExceptions(context, throwables);
				fail("exception above exception limit swallowed");
			} catch (TransactionInvalidException expected) {
				assertSame(throwables.get(0), expected);
			}
		}
	}
	
}
