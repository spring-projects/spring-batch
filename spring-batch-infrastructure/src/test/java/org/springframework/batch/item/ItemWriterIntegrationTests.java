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
