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
package org.springframework.batch.infrastructure.item.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemWriter;

import static org.mockito.Mockito.verify;

/**
 * Test class for {@link SynchronizedItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
public class SynchronizedItemWriterTests {

	@Mock
	private ItemWriter<Object> delegate;

	@Test
	void testDelegateWriteIsCalled() throws Exception {
		// given
		Chunk<Object> chunk = new Chunk<>();
		SynchronizedItemWriter<Object> synchronizedItemWriter = new SynchronizedItemWriter<>(this.delegate);

		// when
		synchronizedItemWriter.write(chunk);

		// then
		verify(this.delegate).write(chunk);
	}

	@Test
	void testNullDelegate() {
		// when
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> new SynchronizedItemWriter<>(null));

		// then
		Assertions.assertEquals("The delegate must not be null", exception.getMessage());
	}

}
