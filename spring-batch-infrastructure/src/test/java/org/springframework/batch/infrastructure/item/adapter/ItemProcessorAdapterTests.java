/*
 * Copyright 2009-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests for {@link ItemProcessorAdapter}.
 *
 * @author Dave Syer
 */
@SpringJUnitConfig(locations = "delegating-item-processor.xml")
class ItemProcessorAdapterTests {

	@Autowired
	private ItemProcessorAdapter<Foo, String> processor;

	@Test
	void testProcess() throws Exception {
		Foo item = new Foo(0, "foo", 1);
		assertEquals("foo", processor.process(item));
	}

}
