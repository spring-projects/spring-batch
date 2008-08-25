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

package org.springframework.batch.retry.support;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.context.RetryContextSupport;

/**
 * @author Dave Syer
 */
public class RetrySynchronizationManagerTests extends TestCase {

	RetryTemplate template = new RetryTemplate();

	protected void setUp() throws Exception {
		super.setUp();
		RetrySynchronizationManagerTests.clearAll();
		RetryContext status = RetrySynchronizationManager.getContext();
		assertNull(status);
	}

	public void testStatusIsStoredByTemplate() throws Exception {

		RetryContext status = RetrySynchronizationManager.getContext();
		assertNull(status);

		template.execute(new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext status) throws Exception {
				RetryContext global = RetrySynchronizationManager.getContext();
				assertNotNull(status);
				assertEquals(global, status);
				return null;
			}
		});

		status = RetrySynchronizationManager.getContext();
		assertNull(status);
	}

	public void testStatusRegistration() throws Exception {
		RetryContext status = new RetryContextSupport(null);
		RetryContext value = RetrySynchronizationManager.register(status);
		assertNull(value);
		value = RetrySynchronizationManager.register(status);
		assertEquals(status, value);
	}

	public void testClear() throws Exception {
		RetryContext status = new RetryContextSupport(null);
		RetryContext value = RetrySynchronizationManager.register(status);
		assertNull(value);
		RetrySynchronizationManager.clear();
		value = RetrySynchronizationManager.register(status);
		assertNull(value);
	}

	public void testParent() throws Exception {
		RetryContext parent = new RetryContextSupport(null);
		RetryContext child = new RetryContextSupport(parent);
		assertSame(parent, child.getParent());
	}

	/**
	 * Clear all contexts starting with the current one and continuing until
	 * {@link RetrySynchronizationManager#clear()} returns null.
	 */
	public static RetryContext clearAll() {
		RetryContext result = null;
		RetryContext context = RetrySynchronizationManager.clear();
		while (context != null) {
			result = context;
			context = RetrySynchronizationManager.clear();
		}
		return result;
	}
}
