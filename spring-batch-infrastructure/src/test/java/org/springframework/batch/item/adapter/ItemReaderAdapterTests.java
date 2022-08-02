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
package org.springframework.batch.item.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.sample.FooService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ItemReaderAdapter}.
 *
 * @author Robert Kasanicky
 */
@SpringJUnitConfig(locations = "delegating-item-provider.xml")
class ItemReaderAdapterTests {

	@Autowired
	private ItemReaderAdapter<Foo> provider;

	@Autowired
	private FooService fooService;

	/*
	 * Regular usage scenario - items are retrieved from the service injected invoker
	 * points to.
	 */
	@Test
	void testNext() throws Exception {
		List<Object> returnedItems = new ArrayList<>();
		Object item;
		while ((item = provider.read()) != null) {
			returnedItems.add(item);
		}

		List<Foo> input = fooService.getGeneratedFoos();
		assertEquals(input.size(), returnedItems.size());
		assertFalse(returnedItems.isEmpty());

		for (int i = 0; i < input.size(); i++) {
			assertSame(input.get(i), returnedItems.get(i));
		}
	}

}
