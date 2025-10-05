/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.support.builder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemWriterBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Mahmoud Ben Hassine
 */
class CompositeItemWriterBuilderTests {

	@Test
	@SuppressWarnings("unchecked")
	void testProcess() throws Exception {

		final int NUMBER_OF_WRITERS = 10;
		Chunk<Object> data = Chunk.of(new Object());

		List<ItemWriter<? super Object>> writers = new ArrayList<>();

		for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
			ItemWriter<? super Object> writer = mock();
			writers.add(writer);
		}
		CompositeItemWriter<Object> itemWriter = new CompositeItemWriterBuilder<>().delegates(writers).build();
		itemWriter.write(data);

		for (ItemWriter<? super Object> writer : writers) {
			verify(writer).write(data);
		}

	}

	@Test
	@SuppressWarnings("unchecked")
	void testProcessVarargs() throws Exception {

		Chunk<Object> data = Chunk.of(new Object());

		List<ItemWriter<? super Object>> writers = new ArrayList<>();

		ItemWriter<? super Object> writer1 = mock();
		writers.add(writer1);
		ItemWriter<? super Object> writer2 = mock();
		writers.add(writer2);

		CompositeItemWriter<Object> itemWriter = new CompositeItemWriterBuilder<>().delegates(writer1, writer2).build();
		itemWriter.write(data);

		for (ItemWriter<? super Object> writer : writers) {
			verify(writer).write(data);
		}

	}

	@Test
	void isStreamOpen() throws Exception {
		ignoreItemStream(false);
		ignoreItemStream(true);
	}

	@SuppressWarnings("unchecked")
	private void ignoreItemStream(boolean ignoreItemStream) throws Exception {
		ItemStreamWriter<? super Object> writer = mock();
		Chunk<Object> data = Chunk.of(new Object());
		ExecutionContext executionContext = new ExecutionContext();

		List<ItemWriter<? super Object>> writers = new ArrayList<>();
		writers.add(writer);
		CompositeItemWriter<Object> itemWriter = new CompositeItemWriterBuilder<>().delegates(writers)
			.ignoreItemStream(ignoreItemStream)
			.build();
		itemWriter.open(executionContext);

		int openCount = 0;
		if (!ignoreItemStream) {
			openCount = 1;
		}
		// If user has set ignoreItemStream to true, then it is expected that they opened
		// the delegate writer.
		verify(writer, times(openCount)).open(executionContext);
		itemWriter.write(data);
	}

}
