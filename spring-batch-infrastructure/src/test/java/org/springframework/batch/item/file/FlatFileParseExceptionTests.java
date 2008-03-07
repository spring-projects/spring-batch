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

package org.springframework.batch.item.file;

import org.springframework.batch.support.AbstractExceptionTests;


public class FlatFileParseExceptionTests extends AbstractExceptionTests {

	public Exception getException(String msg) throws Exception {
		return new FlatFileParseException(msg, "bar");
	}

	public Exception getException(String msg, Throwable t) throws Exception {
		return new FlatFileParseException(msg, t, "bar", 100);
	}
	
	public void testMessageInputLineCount() throws Exception {
		FlatFileParseException exception = new FlatFileParseException("foo", "bar", 100);
		assertEquals("foo", exception.getMessage());
		assertEquals("bar", exception.getInput());
		assertEquals(100, exception.getLineNumber());
	}
	
}
