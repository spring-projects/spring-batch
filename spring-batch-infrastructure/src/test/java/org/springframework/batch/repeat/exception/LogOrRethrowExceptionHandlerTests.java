/*
 * Copyright 2006-2007 the original author or authors.
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

import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.classify.ClassifierSupport;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.LogOrRethrowExceptionHandler.Level;

public class LogOrRethrowExceptionHandlerTests extends TestCase {

	private LogOrRethrowExceptionHandler handler = new LogOrRethrowExceptionHandler();

	private StringWriter writer;

	private RepeatContext context = null;

    @Override
	protected void setUp() throws Exception {
		super.setUp();
		Logger logger = LoggerFactory.getLogger(LogOrRethrowExceptionHandler.class);
		writer = new StringWriter();
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
		Configuration configuration = loggerContext.getConfiguration();

		LoggerConfig rootLoggerConfig = configuration.getLoggerConfig(logger.getName());
		rootLoggerConfig.getAppenders().forEach((name, appender) -> {
			rootLoggerConfig.removeAppender(name);
		});
		Appender appender = WriterAppender.createAppender(PatternLayout.createDefaultLayout(), null, writer,"TESTWriter", false, false);
		rootLoggerConfig.addAppender(appender, org.apache.logging.log4j.Level.DEBUG, null);
	}

	public void testRuntimeException() throws Throwable {
		try {
			handler.handleException(context, new RuntimeException("Foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testError() throws Throwable {
		try {
			handler.handleException(context, new Error("Foo"));
			fail("Expected Error");
		}
		catch (Error e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	public void testNotRethrownErrorLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable,Level>(Level.RETHROW) {
            @Override
			public Level classify(Throwable throwable) {
				return Level.ERROR;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	@SuppressWarnings("serial")
	public void testNotRethrownWarnLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable,Level>(Level.RETHROW) {
            @Override
			public Level classify(Throwable throwable) {
				return Level.WARN;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	@SuppressWarnings("serial")
	public void testNotRethrownDebugLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable,Level>(Level.RETHROW) {
            @Override
			public Level classify(Throwable throwable) {
				return Level.DEBUG;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

}
