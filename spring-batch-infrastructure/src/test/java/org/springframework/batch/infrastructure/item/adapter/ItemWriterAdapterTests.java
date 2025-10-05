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
package org.springframework.batch.infrastructure.item.adapter;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.batch.infrastructure.item.sample.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ItemWriterAdapter}.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = "delegating-item-writer.xml")
class ItemWriterAdapterTests {

	@Autowired
	private ItemWriter<Foo> processor;

	@Autowired
	private FooService fooService;

	/*
	 * Regular usage scenario - input object should be passed to the service the injected
	 * invoker points to.
	 */
	@Test
	void testProcess() throws Exception {
		Foo foo;
		Chunk<Foo> foos = new Chunk<>();
		while ((foo = fooService.generateFoo()) != null) {
			foos.add(foo);
		}
		processor.write(foos);

		List<Foo> input = fooService.getGeneratedFoos();
		List<Foo> processed = fooService.getProcessedFoos();
		assertEquals(input.size(), processed.size());
		assertFalse(fooService.getProcessedFoos().isEmpty());

		for (int i = 0; i < input.size(); i++) {
			assertSame(input.get(i), processed.get(i));
		}

	}

}
