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

import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.exception.LogOrRethrowExceptionHandler;
import org.springframework.batch.support.ExceptionClassifierSupport;

public class LogOrRethrowExceptionHandlerTests extends TestCase {

	private LogOrRethrowExceptionHandler handler = new LogOrRethrowExceptionHandler();
	private StringWriter writer;
	private RepeatContext context = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		Logger logger = Logger.getLogger(LogOrRethrowExceptionHandler.class);
		logger.setLevel(Level.DEBUG);
		writer = new StringWriter();
		logger.removeAllAppenders();
		logger.getParent().removeAllAppenders();
		logger.addAppender(new WriterAppender(new SimpleLayout(), writer));
	}
	
	public void testRuntimeException() throws Exception {
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testError() throws Exception {
		try {
			handler.handleException(context, new Error("Foo"));
			fail("Expected BatchException");
		} catch (RepeatException e) {
			assertEquals("Foo", e.getCause().getMessage());
		}
	}
	
	public void testNotRethrownErrorLevel() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return LogOrRethrowExceptionHandler.ERROR;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	public void testNotRethrownWarnLevel() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return LogOrRethrowExceptionHandler.WARN;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}
	
	public void testNotRethrownDebugLevel() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return LogOrRethrowExceptionHandler.DEBUG;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	public void testUnclassifiedException() throws Exception {
		handler.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				return "DEFAULT";
			}
		});
		try {
			handler.handleException(context, new Error("Foo"));
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().toLowerCase().indexOf("unclassified")>=0);
		}
	}

}
