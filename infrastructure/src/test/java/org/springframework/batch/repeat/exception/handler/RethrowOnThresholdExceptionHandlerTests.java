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

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.common.ExceptionClassifierSupport;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextCounter;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.exception.RepeatException;

public class RethrowOnThresholdExceptionHandlerTests extends TestCase {

	private RethrowOnThresholdExceptionHandler handler = new RethrowOnThresholdExceptionHandler();
	private RepeatContext parent = new RepeatContextSupport(null);
	private RepeatContext context = new RepeatContextSupport(parent);
	
	public void testRuntimeException() throws Exception {
		try {
			handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testError() throws Exception {
		try {
			handler.handleExceptions(context, Collections.singleton(new Error("Foo")));
			fail("Expected BatchException");
		} catch (RepeatException e) {
			assertEquals("Foo", e.getCause().getMessage());
		}
	}
	
	public void testNotRethrownWithThreshold() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap("RuntimeException", new Integer(1)));
		// No exception...
		handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
		RepeatContextCounter counter = new RepeatContextCounter(context, RethrowOnThresholdExceptionHandler.class + ".RuntimeException");
		assertNotNull(counter);
		assertEquals(1, counter.getCount());
	}
	
	public void testRethrowOnThreshold() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap("RuntimeException", new Integer(1)));
		// No exception...
		handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
		try {
			handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testNonIntegerAsThreshold() throws Exception {
		try {
			handler.setThresholds(Collections.singletonMap("RuntimeException", new Long(1)));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}
	
	public void testNotUseParent() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap("RuntimeException", new Integer(1)));
		// No exception...
		handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
		context = new RepeatContextSupport(parent);
		try {
			// No exception again - context is changed...
			handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
		}
		catch (RuntimeException e) {
			fail("Unexpected Error");
		}
	}

	public void testUseParent() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap("RuntimeException", new Integer(1)));
		handler.setUseParent(true);
		// No exception...
		handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
		context = new RepeatContextSupport(parent);
		try {
			handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
			fail("Expected Error");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}
	
	public void testNotStringAsKey() throws Exception {
		try {
			handler.setThresholds(Collections.singletonMap(RuntimeException.class, new Integer(1)));
			// It's not an error, but not advised...
		}
		catch (RuntimeException e) {
			throw e;
		}
		
	}

}
