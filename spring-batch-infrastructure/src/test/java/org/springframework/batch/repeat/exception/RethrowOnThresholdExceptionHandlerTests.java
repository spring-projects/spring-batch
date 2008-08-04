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

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextCounter;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.support.ExceptionClassifierSupport;

public class RethrowOnThresholdExceptionHandlerTests extends TestCase {

	private RethrowOnThresholdExceptionHandler handler = new RethrowOnThresholdExceptionHandler();
	private RepeatContext parent = new RepeatContextSupport(null);
	private RepeatContext context = new RepeatContextSupport(parent);
	
	public void testRuntimeException() throws Throwable {
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testError() throws Throwable {
		try {
			handler.handleException(context, new Error("Foo"));
			fail("Expected Error");
		} catch (Error e) {
			assertEquals("Foo", e.getMessage());
		}
	}
	
	public void testNotRethrownWithThreshold() throws Throwable {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public String classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap((Object)"RuntimeException", new Integer(1)));
		// No exception...
		handler.handleException(context, new RuntimeException("Foo"));
		RepeatContextCounter counter = new RepeatContextCounter(context, RethrowOnThresholdExceptionHandler.class.getName() + ".RuntimeException");
		assertNotNull(counter);
		assertEquals(1, counter.getCount());
	}
	
	public void testRethrowOnThreshold() throws Throwable {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public String classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap((Object)"RuntimeException", new Integer(2)));
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
	
	public void testNotUseParent() throws Throwable {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public String classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap((Object)"RuntimeException", new Integer(1)));
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

	public void testUseParent() throws Throwable {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public String classify(Throwable throwable) {
				return "RuntimeException";
			}
		});
		handler.setThresholds(Collections.singletonMap((Object)"RuntimeException", new Integer(1)));
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
	
	public void testNotStringAsKey() throws Exception {
		try {
			handler.setThresholds(Collections.singletonMap((Object)RuntimeException.class, new Integer(1)));
			// It's not an error, but not advised...
		}
		catch (RuntimeException e) {
			throw e;
		}
		
	}

}
