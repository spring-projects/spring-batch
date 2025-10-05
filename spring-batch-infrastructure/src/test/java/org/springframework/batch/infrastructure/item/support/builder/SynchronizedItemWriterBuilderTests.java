/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item.support.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemWriterBuilder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for {@link SynchronizedItemWriterBuilder}.
 *
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
public class SynchronizedItemWriterBuilderTests {

	@Mock
	private ItemWriter<Object> delegate;

	@Test
	void testSynchronizedItemWriterCreation() {
		// when
		SynchronizedItemWriter<Object> synchronizedItemWriter = new SynchronizedItemWriterBuilder<>()
			.delegate(this.delegate)
			.build();

		// then
		Object delegateField = ReflectionTestUtils.getField(synchronizedItemWriter, "delegate");
		Assertions.assertEquals(delegateField, this.delegate);
	}

	@Test
	void testSynchronizedItemWriterCreationWithNullDelegate() {
		// when
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> new SynchronizedItemWriterBuilder<>().delegate(null).build());

		// then
		Assertions.assertEquals("A delegate is required", exception.getMessage());
	}

}
