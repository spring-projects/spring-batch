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

package org.springframework.batch.retry.interceptor;

import junit.framework.TestCase;

public class StatisticsRetryInterceptorTests extends TestCase {

	StatisticsRetryInterceptor interceptor = new StatisticsRetryInterceptor();

	public void testGetAbortCount() {
		assertEquals(0, interceptor.getAbortCount());
		interceptor.close(null, null, null);
		assertEquals(0, interceptor.getAbortCount());
		interceptor.close(null, null, new Exception());
		assertEquals(1, interceptor.getAbortCount());
	}

	public void testGetCompleteCount() {
		assertEquals(0, interceptor.getCompleteCount());
		interceptor.close(null, null, null);
		assertEquals(1, interceptor.getCompleteCount());
	}

	public void testGetErrorCount() {
		assertEquals(0, interceptor.getErrorCount());
		interceptor.onError(null, null, null);
		assertEquals(1, interceptor.getErrorCount());
	}

	public void testGetStartedCount() {
		assertEquals(0, interceptor.getStartedCount());
		interceptor.open(null, null);
		assertEquals(1, interceptor.getStartedCount());
	}

	public void testGetName() {
		assertNotNull(interceptor.getName());
		interceptor.setName("foo");
		assertEquals("foo", interceptor.getName());
	}

}
