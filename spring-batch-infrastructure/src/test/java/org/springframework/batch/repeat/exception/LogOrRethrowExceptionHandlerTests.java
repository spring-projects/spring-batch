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

import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.classify.ClassifierSupport;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.LogOrRethrowExceptionHandler.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogOrRethrowExceptionHandlerTests {

	private final LogOrRethrowExceptionHandler handler = new LogOrRethrowExceptionHandler();

	private final StringWriter writer = new StringWriter();

	private final RepeatContext context = null;

	@BeforeEach
	void setUp() {
		Logger logger = LoggerFactory.getLogger(LogOrRethrowExceptionHandler.class);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
		Configuration configuration = loggerContext.getConfiguration();

		LoggerConfig rootLoggerConfig = configuration.getLoggerConfig(logger.getName());
		rootLoggerConfig.getAppenders().forEach((name, appender) -> rootLoggerConfig.removeAppender(name));
		Appender appender = WriterAppender.createAppender(PatternLayout.createDefaultLayout(), null, writer,
				"TESTWriter", false, false);
		rootLoggerConfig.addAppender(appender, org.apache.logging.log4j.Level.DEBUG, null);
	}

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
	void testNotRethrownErrorLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable, Level>(Level.RETHROW) {
			@Override
			public Level classify(Throwable throwable) {
				return Level.ERROR;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	@Test
	void testNotRethrownWarnLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable, Level>(Level.RETHROW) {
			@Override
			public Level classify(Throwable throwable) {
				return Level.WARN;
			}
		});
		// No exception...
		handler.handleException(context, new Error("Foo"));
		assertNotNull(writer.toString());
	}

	@Test
	void testNotRethrownDebugLevel() throws Throwable {
		handler.setExceptionClassifier(new ClassifierSupport<Throwable, Level>(Level.RETHROW) {
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
