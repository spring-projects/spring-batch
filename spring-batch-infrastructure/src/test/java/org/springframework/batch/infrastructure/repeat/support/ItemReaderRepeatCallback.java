/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.repeat.support;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatCallback;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class ItemReaderRepeatCallback<T> implements RepeatCallback {

	private final ItemReader<T> reader;

	private final ItemWriter<T> writer;

	public ItemReaderRepeatCallback(ItemReader<T> reader, ItemWriter<T> writer) {
		this.writer = writer;
		this.reader = reader;
	}

	@Override
	public RepeatStatus doInIteration(RepeatContext context) throws Exception {
		T item = reader.read();
		if (item == null) {
			return RepeatStatus.FINISHED;
		}
		writer.write(Chunk.of(item));
		return RepeatStatus.CONTINUABLE;
	}

}
