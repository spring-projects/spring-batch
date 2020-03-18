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

import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ItemReadListenerAdapterTests {

	private ItemReadListenerAdapter<String> adapter;
	@Mock
	private ItemReadListener delegate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		adapter = new ItemReadListenerAdapter<>(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullDelegate() {
		adapter = new ItemReadListenerAdapter<>(null);
	}

	@Test
	public void testBeforeRead() throws Exception {
		adapter.beforeRead();

		verify(delegate).beforeRead();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testBeforeReadException() throws Exception {
		doThrow(new Exception("Should occur")).when(delegate).beforeRead();

		adapter.beforeRead();
	}

	@Test
	public void testAfterRead() throws Exception {
		String item = "item";

		adapter.afterRead(item);

		verify(delegate).afterRead(item);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterReadException() throws Exception {
		String item = "item";
		Exception expected = new Exception("expected");

		doThrow(expected).when(delegate).afterRead(item);

		adapter.afterRead(item);
	}

	@Test
	public void testOnReadError() throws Exception {
		Exception cause = new Exception ("cause");

		adapter.onReadError(cause);

		verify(delegate).onReadError(cause);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testOnReadErrorException() throws Exception {
		Exception cause = new Exception ("cause");
		Exception result = new Exception("result");

		doThrow(result).when(delegate).onReadError(cause);

		adapter.onReadError(cause);
	}
}
