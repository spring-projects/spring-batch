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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.repeat.exception.CompositeExceptionHandler;
import org.springframework.batch.infrastructure.repeat.exception.ExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeExceptionHandlerTests {

	private final CompositeExceptionHandler handler = new CompositeExceptionHandler();

	@Test
	void testNewHandler() {
		assertDoesNotThrow(() -> handler.handleException(null, new RuntimeException()));
	}

	@Test
	void testDelegation() throws Throwable {
		final List<String> list = new ArrayList<>();
		handler.setHandlers(new ExceptionHandler[] { (context, throwable) -> list.add("1"),
				(context, throwable) -> list.add("2") });
		handler.handleException(null, new RuntimeException());
		assertEquals(2, list.size());
		assertEquals("1", list.get(0));
		assertEquals("2", list.get(1));
	}

}
