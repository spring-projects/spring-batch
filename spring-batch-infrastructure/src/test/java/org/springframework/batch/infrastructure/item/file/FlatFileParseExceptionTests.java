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

package org.springframework.batch.infrastructure.item.file;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.common.AbstractExceptionTests;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatFileParseExceptionTests extends AbstractExceptionTests {

	@Override
	protected Exception getException(String msg) {
		return new FlatFileParseException(msg, "bar");
	}

	@Override
	protected Exception getException(String msg, Throwable t) {
		return new FlatFileParseException(msg, t, "bar", 100);
	}

	@Test
	void testMessageInputLineCount() {
		FlatFileParseException exception = new FlatFileParseException("foo", "bar", 100);
		assertEquals("foo", exception.getMessage());
		assertEquals("bar", exception.getInput());
		assertEquals(100, exception.getLineNumber());
	}

}
