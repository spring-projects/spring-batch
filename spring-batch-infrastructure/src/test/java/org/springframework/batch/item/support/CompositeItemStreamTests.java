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
package org.springframework.batch.item.support;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 *
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
