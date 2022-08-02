/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.support.builder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 */
@ExtendWith(MockitoExtension.class)
class CompositeItemProcessorBuilderTests {

	@Mock
	private ItemProcessor<Object, Object> processor1;

	@Mock
	private ItemProcessor<Object, Object> processor2;

	private List<ItemProcessor<Object, Object>> processors;

	@BeforeEach
	void setup() {
		this.processors = new ArrayList<>();
		this.processors.add(processor1);
		this.processors.add(processor2);
	}

	@Test
	void testTransform() throws Exception {
		Object item = new Object();
		Object itemAfterFirstTransformation = new Object();
		Object itemAfterSecondTransformation = new Object();
		CompositeItemProcessor<Object, Object> composite = new CompositeItemProcessorBuilder<>()
				.delegates(this.processors).build();

		when(processor1.process(item)).thenReturn(itemAfterFirstTransformation);
		when(processor2.process(itemAfterFirstTransformation)).thenReturn(itemAfterSecondTransformation);

		assertSame(itemAfterSecondTransformation, composite.process(item));
	}

	@Test
	void testTransformVarargs() throws Exception {
		Object item = new Object();
		Object itemAfterFirstTransformation = new Object();
		Object itemAfterSecondTransformation = new Object();
		CompositeItemProcessor<Object, Object> composite = new CompositeItemProcessorBuilder<>()
				.delegates(this.processor1, this.processor2).build();

		when(processor1.process(item)).thenReturn(itemAfterFirstTransformation);
		when(processor2.process(itemAfterFirstTransformation)).thenReturn(itemAfterSecondTransformation);

		assertSame(itemAfterSecondTransformation, composite.process(item));
	}

	@Test
	void testNullOrEmptyDelegates() {
		validateExceptionMessage(new CompositeItemProcessorBuilder<>().delegates(new ArrayList<>()),
				"The delegates list must have one or more delegates.");
		validateExceptionMessage(new CompositeItemProcessorBuilder<>().delegates(),
				"The delegates list must have one or more delegates.");
		validateExceptionMessage(new CompositeItemProcessorBuilder<>(), "A list of delegates is required.");
	}

	private void validateExceptionMessage(CompositeItemProcessorBuilder<?, ?> builder, String message) {
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals(message, exception.getMessage());
	}

}
