/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.support;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.CompositeItemReader;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link CompositeItemReader}.
 *
 * @author Mahmoud Ben Hassine
 * @author Elimelec Burghelea
 */
public class CompositeItemReaderTests {

	@Test
	void testCompositeItemReaderOpen() {
		// given
		ItemStreamReader<String> reader1 = mock();
		ItemStreamReader<String> reader2 = mock();
		CompositeItemReader<String> compositeItemReader = new CompositeItemReader<>(Arrays.asList(reader1, reader2));
		ExecutionContext executionContext = new ExecutionContext();

		// when
		compositeItemReader.open(executionContext);

		// then
		verify(reader1).open(executionContext);
		verify(reader2).open(executionContext);
	}

	@Test
	void testCompositeItemReaderRead() throws Exception {
		// given
		ItemStreamReader<String> reader1 = mock();
		ItemStreamReader<String> reader2 = mock();
		CompositeItemReader<String> compositeItemReader = new CompositeItemReader<>(Arrays.asList(reader1, reader2));
		when(reader1.read()).thenReturn("foo1", "foo2", null);
		when(reader2.read()).thenReturn("bar1", "bar2", null);

		// when & then
		compositeItemReader.read();
		verify(reader1, times(1)).read();
		compositeItemReader.read();
		verify(reader1, times(2)).read();
		compositeItemReader.read();
		verify(reader1, times(3)).read();

		compositeItemReader.read();
		verify(reader2, times(2)).read();
		compositeItemReader.read();
		verify(reader2, times(3)).read();
		compositeItemReader.read();
		verify(reader2, times(3)).read();
	}

	@Test
	void testCompositeItemReaderUpdate() {
		// given
		ItemStreamReader<String> reader1 = mock();
		ItemStreamReader<String> reader2 = mock();
		CompositeItemReader<String> compositeItemReader = new CompositeItemReader<>(Arrays.asList(reader1, reader2));
		ExecutionContext executionContext = new ExecutionContext();

		// when
		compositeItemReader.update(executionContext);

		// then
		verify(reader1).update(executionContext);
		verifyNoInteractions(reader2); // reader1 is the current delegate in this setup
	}

	@Test
	void testCompositeItemReaderClose() {
		// given
		ItemStreamReader<String> reader1 = mock();
		ItemStreamReader<String> reader2 = mock();
		CompositeItemReader<String> compositeItemReader = new CompositeItemReader<>(Arrays.asList(reader1, reader2));

		// when
		compositeItemReader.close();

		// then
		verify(reader1).close();
		verify(reader2).close();
	}

	@Test
	void testCompositeItemReaderCloseWithDelegateThatThrowsException() {
		// given
		ItemStreamReader<String> reader1 = mock();
		ItemStreamReader<String> reader2 = mock();
		CompositeItemReader<String> compositeItemReader = new CompositeItemReader<>(Arrays.asList(reader1, reader2));

		doThrow(new ItemStreamException("A failure")).when(reader1).close();

		// when
		try {
			compositeItemReader.close();
			Assertions.fail("Expected an ItemStreamException");
		}
		catch (ItemStreamException ignored) {

		}

		// then
		verify(reader1).close();
		verify(reader2).close();
	}

}