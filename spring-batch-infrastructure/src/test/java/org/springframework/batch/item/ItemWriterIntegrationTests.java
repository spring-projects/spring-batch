/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.ListItemWriter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.item.ItemWriter.mapping;

class ItemWriterIntegrationTests {

	@Test
	void testMappingWithCompositeItemWriter() throws Exception {
		ListItemWriter<String> nameItemWriter = new ListItemWriter<>();
		ListItemWriter<Integer> ageItemWriter = new ListItemWriter<>();

		record Person(String name, int age) {
		}

		ItemWriter<Person> personItemWriter = new CompositeItemWriter<>(List.of( //
				mapping(Person::name, nameItemWriter), //
				mapping(Person::age, ageItemWriter)));

		personItemWriter.write(Chunk.of(new Person("Foo", 42), new Person("Bar", 24)));
		personItemWriter.write(Chunk.of(new Person("Baz", 21), new Person("Qux", 12)));

		assertThat(nameItemWriter.getWrittenItems()).containsExactly("Foo", "Bar", "Baz", "Qux");
		assertThat(ageItemWriter.getWrittenItems()).containsExactly(42, 24, 21, 12);
	}

}
