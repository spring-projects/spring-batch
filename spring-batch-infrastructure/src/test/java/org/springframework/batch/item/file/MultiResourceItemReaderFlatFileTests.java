/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.item.file;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

class MultiResourceItemReaderFlatFileTests extends AbstractItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {

		LineMapper<Foo> fooLineMapper = (line, lineNumber) -> {
			Foo foo = new Foo();
			foo.setValue(Integer.parseInt(line));
			return foo;
		};
		FlatFileItemReader<Foo> fileReader = new FlatFileItemReader<>(fooLineMapper);
		fileReader.setSaveState(true);

		MultiResourceItemReader<Foo> multiReader = new MultiResourceItemReader<>(fileReader);

		Resource r1 = new ByteArrayResource("1\n2\n".getBytes());
		Resource r2 = new ByteArrayResource("".getBytes());
		Resource r3 = new ByteArrayResource("3\n".getBytes());
		Resource r4 = new ByteArrayResource("4\n5\n".getBytes());

		multiReader.setResources(new Resource[] { r1, r2, r3, r4 });
		multiReader.setSaveState(true);
		multiReader.setComparator((arg0, arg1) -> {
			return 0; // preserve original ordering
		});

		return multiReader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiResourceItemReader<Foo> multiReader = (MultiResourceItemReader<Foo>) tested;
		multiReader.close();
		multiReader.setResources(new Resource[] { new ByteArrayResource("".getBytes()) });
		multiReader.open(new ExecutionContext());
	}

}
