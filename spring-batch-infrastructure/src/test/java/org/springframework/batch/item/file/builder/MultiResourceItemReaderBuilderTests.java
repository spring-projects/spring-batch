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

package org.springframework.batch.item.file.builder;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
class MultiResourceItemReaderBuilderTests extends AbstractItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {

		FlatFileItemReader<Foo> fileReader = new FlatFileItemReader<>();

		fileReader.setLineMapper((line, lineNumber) -> {
			Foo foo = new Foo();
			foo.setValue(Integer.valueOf(line));
			return foo;
		});
		fileReader.setSaveState(true);

		Resource r1 = new ByteArrayResource("1\n2\n".getBytes());
		Resource r2 = new ByteArrayResource("".getBytes());
		Resource r3 = new ByteArrayResource("3\n".getBytes());
		Resource r4 = new ByteArrayResource("4\n5\n".getBytes());

		Comparator<Resource> comparator = (arg0, arg1) -> {
			return 0; // preserve original ordering
		};
		return new MultiResourceItemReaderBuilder<Foo>().delegate(fileReader)
				.resources(new Resource[] { r1, r2, r3, r4 }).saveState(true).comparator(comparator).name("FOO")
				.build();
	}

	@Test
	void testNullDelegate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new MultiResourceItemReaderBuilder<String>().resources(new Resource[] {}).build());
		assertEquals("delegate is required.", exception.getMessage());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testNullResources() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new MultiResourceItemReaderBuilder<String>().delegate(mock(FlatFileItemReader.class)).build());
		assertEquals("resources array is required.", exception.getMessage());
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiResourceItemReader<Foo> multiReader = (MultiResourceItemReader<Foo>) tested;
		multiReader.close();
		multiReader.setResources(new Resource[] { new ByteArrayResource("".getBytes()) });
		multiReader.open(new ExecutionContext());
	}

}
