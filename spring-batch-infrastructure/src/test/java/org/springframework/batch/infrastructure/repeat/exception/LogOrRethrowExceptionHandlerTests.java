/*
 * Copyright 2006-2023 the original author or authors.
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

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.exception.LogOrRethrowExceptionHandler;
import org.springframework.batch.infrastructure.repeat.exception.LogOrRethrowExceptionHandler.Level;
import org.springframework.classify.ClassifierSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogOrRethrowExceptionHandlerTests {

	private final LogOrRethrowExceptionHandler handler = new LogOrRethrowExceptionHandler();

	private final StringWriter writer = new StringWriter();

	private final RepeatContext context = null;

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
		handler.setExceptionClassifier(new ClassifierSupport<>(Level.RETHROW) {
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
		handler.setExceptionClassifier(new ClassifierSupport<>(Level.RETHROW) {
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
		handler.setExceptionClassifier(new ClassifierSupport<>(Level.RETHROW) {
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
