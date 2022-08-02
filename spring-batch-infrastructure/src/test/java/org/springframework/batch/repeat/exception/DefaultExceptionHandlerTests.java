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

import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultExceptionHandlerTests {

	private final DefaultExceptionHandler handler = new DefaultExceptionHandler();

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

}
