/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.jsr.item;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.batch.api.chunk.ItemProcessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ItemProcessorAdapterTests {

	private ItemProcessorAdapter<String, String> adapter;
	@Mock
	private ItemProcessor delegate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new ItemProcessorAdapter<>(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new ItemProcessorAdapter<>(null);
	}

	@Test
	public void testProcess() throws Exception {
		String input = "input";
		String output = "output";

		when(delegate.processItem(input)).thenReturn(output);

		assertEquals(output, adapter.process(input));
	}
}
