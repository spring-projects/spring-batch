/*
 * Copyright 2006-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.support.CompositeItemStream;

/**
 * @author Dave Syer
 * @author Elimelec Burghelea
 */
class CompositeItemStreamTests {

	private final CompositeItemStream manager = new CompositeItemStream();

	private final List<String> list = new ArrayList<>();

	@Test
	void testRegisterAndOpen() {
		ItemStreamSupport stream = new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				list.add("bar");
			}
		};
		manager.register(stream);
		manager.open(null);
		assertEquals(1, list.size());
	}

	@Test
	void testRegisterTwice() {
		ItemStreamSupport stream = new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				list.add("bar");
			}
		};
		manager.register(stream);
		manager.register(stream);
		manager.open(null);
		assertEquals(1, list.size());
	}

	@Test
	void testMark() {
		manager.register(new ItemStreamSupport() {
			@Override
			public void update(ExecutionContext executionContext) {
				super.update(executionContext);
				list.add("bar");
			}
		});
		manager.update(null);
		assertEquals(1, list.size());
	}

	@Test
	void testClose() {
		manager.register(new ItemStreamSupport() {
			@Override
			public void close() {
				super.close();
				list.add("bar");
			}
		});
		manager.close();
		assertEquals(1, list.size());
	}

	@Test
	void testClose2Delegates() {
		ItemStream reader1 = Mockito.mock(ItemStream.class);
		ItemStream reader2 = Mockito.mock(ItemStream.class);
		manager.register(reader1);
		manager.register(reader2);

		manager.close();

		verify(reader1, times(1)).close();
		verify(reader2, times(1)).close();
	}

	@Test
	void testClose2DelegatesThatThrowsException() {
		ItemStream reader1 = Mockito.mock(ItemStream.class);
		ItemStream reader2 = Mockito.mock(ItemStream.class);
		manager.register(reader1);
		manager.register(reader2);

		doThrow(new ItemStreamException("A failure")).when(reader1).close();

		try {
			manager.close();
			Assertions.fail("Expected an ItemStreamException");
		}
		catch (ItemStreamException ignored) {

		}

		verify(reader1, times(1)).close();
		verify(reader2, times(1)).close();
	}

	@Test
	void testCloseDoesNotUnregister() {
		manager.setStreams(new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				list.add("bar");
			}
		} });
		manager.open(null);
		manager.close();
		manager.open(null);
		assertEquals(2, list.size());
	}

}
