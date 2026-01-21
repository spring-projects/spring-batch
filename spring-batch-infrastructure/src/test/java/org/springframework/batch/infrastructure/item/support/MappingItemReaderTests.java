/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chirag Tailor
 */
class MappingItemReaderTests {

	@Test
	void mapsTheItemsReadByTheUpstreamItemReader() throws Exception {
		// given
		ListItemReader<TestUpstreamItem> upstreamItemReader = new ListItemReader<>(List.of(new TestUpstreamItem(3)));
		MappingItemReader<TestUpstreamItem, TestItem> mappingItemReader = new MappingItemReader<>(upstreamItemReader,
				testUpstreamItem -> new TestItem(testUpstreamItem.number() * 2));

		// when
		TestItem testItem = mappingItemReader.read();

		// then
		assertThat(testItem).isNotNull();
		assertThat(testItem.number()).isEqualTo(6);
	}

	@Test
	void terminatesWhenThereAreNoMoreUpstreamItems() throws Exception {
		// given
		ListItemReader<TestUpstreamItem> upstreamItemReader = new ListItemReader<>(List.of());
		MappingItemReader<TestUpstreamItem, TestItem> mappingItemReader = new MappingItemReader<>(upstreamItemReader,
				testUpstreamItem -> new TestItem(testUpstreamItem.number() * 2));

		// when
		TestItem testItem = mappingItemReader.read();

		// then
		assertThat(testItem).isNull();
	}

	private record TestUpstreamItem(int number) {
	}

	private record TestItem(int number) {
	}

}
