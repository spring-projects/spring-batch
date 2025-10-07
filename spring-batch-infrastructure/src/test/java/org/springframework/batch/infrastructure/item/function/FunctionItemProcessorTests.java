/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.function;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.function.FunctionItemProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Michael Minella
 */
class FunctionItemProcessorTests {

	private final Function<Object, String> function = Object::toString;

	@Test
	void testConstructorValidation() {
		assertThrows(IllegalArgumentException.class, () -> new FunctionItemProcessor<>(null));
	}

	@Test
	void testFunctionItemProcessor() throws Exception {
		ItemProcessor<Object, String> itemProcessor = new FunctionItemProcessor<>(this.function);

		assertEquals("1", itemProcessor.process(1L));
		assertEquals("foo", itemProcessor.process("foo"));
	}

}
