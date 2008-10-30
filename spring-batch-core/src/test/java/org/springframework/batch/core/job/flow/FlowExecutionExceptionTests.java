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
package org.springframework.batch.core.job.flow;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.job.flow.FlowExecutionException;

/**
 * @author Dave Syer
 *
 */
public class FlowExecutionExceptionTests {

	/**
	 * Test method for {@link FlowExecutionException#FlowExecutionException(String)}.
	 */
	@Test
	public void testFlowExecutionExceptionString() {
		FlowExecutionException exception = new FlowExecutionException("foo");
		assertEquals("foo", exception.getMessage());
	}

	/**
	 * Test method for {@link FlowExecutionException#FlowExecutionException(String, Throwable)}.
	 */
	@Test
	public void testFlowExecutionExceptionStringThrowable() {
		FlowExecutionException exception = new FlowExecutionException("foo", new RuntimeException("bar"));
		assertEquals("foo", exception.getMessage());
		assertEquals("bar", exception.getCause().getMessage());
	}

}
