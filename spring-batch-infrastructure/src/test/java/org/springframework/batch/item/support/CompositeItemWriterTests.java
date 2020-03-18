/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link CompositeItemWriter}
 * 
 * @author Robert Kasanicky
 * @author Will Schipp
 */
public class CompositeItemWriterTests {

	// object under test
	private CompositeItemWriter<Object> itemWriter = new CompositeItemWriter<>();

	/**
	 * Regular usage scenario. All injected processors should be called.
	 */
	@Test
	public void testProcess() throws Exception {

		final int NUMBER_OF_WRITERS = 10;
		List<Object> data = Collections.singletonList(new Object());

		List<ItemWriter<? super Object>> writers = new ArrayList<>();

		for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
			@SuppressWarnings("unchecked")
			ItemWriter<? super Object> writer = mock(ItemWriter.class);

			writer.write(data);

			writers.add(writer);
		}

		itemWriter.setDelegates(writers);
		itemWriter.write(data);


	}

	@Test
	public void testItemStreamCalled() throws Exception {
		doTestItemStream(true);
	}

	@Test
	public void testItemStreamNotCalled() throws Exception {
		doTestItemStream(false);
	}

	private void doTestItemStream(boolean expectOpen) throws Exception {
		@SuppressWarnings("unchecked")
		ItemStreamWriter<? super Object> writer = mock(ItemStreamWriter.class);
		List<Object> data = Collections.singletonList(new Object());
		ExecutionContext executionContext = new ExecutionContext();
		if (expectOpen) {
			writer.open(executionContext);
		}
		writer.write(data);

		List<ItemWriter<? super Object>> writers = new ArrayList<>();
		writers.add(writer);

		itemWriter.setDelegates(writers);
		if (expectOpen) {
			itemWriter.open(executionContext);
		}
		itemWriter.write(data);
	}

}
