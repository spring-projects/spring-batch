/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.batch.item.function;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ItemProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Michael Minella
 */
public class FunctionItemProcessorTests {

	private Function<Object, String> function;

	@Before
	public void setUp() {
		this.function = o -> o.toString();
	}

	@Test
	public void testConstructorValidation() {
		try {
			new FunctionItemProcessor<>(null);
			fail("null should not be accepted as a constructor arg");
		}
		catch (IllegalArgumentException iae) {}
	}

	@Test
	public void testFunctionItemProcessor() throws Exception {
		ItemProcessor<Object, String> itemProcessor =
				new FunctionItemProcessor<>(this.function);

		assertEquals("1", itemProcessor.process(1L));
		assertEquals("foo", itemProcessor.process("foo"));
	}
}
