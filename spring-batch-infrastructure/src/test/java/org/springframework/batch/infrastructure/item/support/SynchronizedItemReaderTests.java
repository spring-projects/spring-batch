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

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemReader;

import static org.mockito.Mockito.verify;

/**
 * Test class for {@link SynchronizedItemReader}.
 *
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
public class SynchronizedItemReaderTests {

	@Mock
	private ItemReader<Object> delegate;

	@Test
	void testDelegateReadIsCalled() throws Exception {
		// given
		SynchronizedItemReader<Object> synchronizedItemReader = new SynchronizedItemReader<>(this.delegate);

		// when
		synchronizedItemReader.read();

		// then
		verify(this.delegate).read();
	}

	@Test
	void testNullDelegate() {
		// when
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> new SynchronizedItemReader<>(null));

		// then
		Assertions.assertEquals("The delegate must not be null", exception.getMessage());
	}

}
