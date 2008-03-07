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

package org.springframework.batch.repeat;

import junit.framework.TestCase;

public abstract class AbstractExceptionTests extends TestCase {

	public void testExceptionString() throws Exception {
		Exception exception = getException("foo");
		assertEquals("foo", exception.getMessage());
	}

	public void testExceptionStringThrowable() throws Exception {
		Exception exception = getException("foo", new IllegalStateException());
		assertEquals("foo", exception.getMessage().substring(0, 3));
	}

	public abstract Exception getException(String msg) throws Exception;

	public abstract Exception getException(String msg, Throwable t) throws Exception;
}
