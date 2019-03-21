/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JobListenerAdapterTests {

	private JobListenerAdapter adapter;
	@Mock
	private JobListener delegate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		adapter = new JobListenerAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new JobListenerAdapter(null);
	}

	@Test
	public void testBeforeJob() throws Exception {
		adapter.beforeJob(null);

		verify(delegate).beforeJob();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testBeforeJobException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).beforeJob();

		adapter.beforeJob(null);
	}

	@Test
	public void testAfterJob() throws Exception {
		adapter.afterJob(null);

		verify(delegate).afterJob();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterJobException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).afterJob();

		adapter.afterJob(null);
	}
}
