/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.item.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemProcessor;

/**
 * Tests for {@link CompositeItemProcessor}.
 *
 * @author Robert Kasanicky
 * @author Will Schipp
 */
class CompositeItemProcessorTests {

	private final CompositeItemProcessor<Object, Object> composite = new CompositeItemProcessor<>();

	private ItemProcessor<Object, Object> processor1;

	private ItemProcessor<Object, Object> processor2;

	@BeforeEach
	void setUp() throws Exception {
		processor1 = mock();
		processor2 = mock();

		composite.setDelegates(Arrays.asList(processor1, processor2));

		composite.afterPropertiesSet();
	}

	/**
	 * Regular usage scenario - item is passed through the processing chain, return value
	 * of the of the last transformation is returned by the composite.
	 */
	@Test
	void testTransform() throws Exception {
		Object item = new Object();
		Object itemAfterFirstTransformation = new Object();
		Object itemAfterSecondTransformation = new Object();

		when(processor1.process(item)).thenReturn(itemAfterFirstTransformation);

		when(processor2.process(itemAfterFirstTransformation)).thenReturn(itemAfterSecondTransformation);

		assertSame(itemAfterSecondTransformation, composite.process(item));

	}

	/**
	 * Test that the CompositeItemProcessor can work with generic types for the
	 * ItemProcessor delegates.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void testItemProcessorGenerics() throws Exception {
		CompositeItemProcessor<String, String> composite = new CompositeItemProcessor<>();
		final ItemProcessor<String, Integer> processor1 = mock();
		final ItemProcessor<Integer, String> processor2 = mock();
		composite.setDelegates(Arrays.asList(processor1, processor2));

		composite.afterPropertiesSet();

		when(processor1.process("input")).thenReturn(5);

		when(processor2.process(5)).thenReturn("output");

		assertEquals("output", composite.process("input"));

	}

	@Test
	void testFilteredItemInFirstProcessor() throws Exception {

		Object item = new Object();
		when(processor1.process(item)).thenReturn(null);
		assertNull(composite.process(item));
	}

}
