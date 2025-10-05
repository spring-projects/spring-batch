/*
 * Copyright 2014-2024 the original author or authors.
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

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Item writer that writes items to a <code>List</code>.
 *
 * <p>
 * This writer is <b>not</b> thread-safe.
 * </p>
 *
 * @author mminella
 * @author Mahmoud Ben Hassine
 */
public class ListItemWriter<T> implements ItemWriter<T> {

	private final List<T> writtenItems = new ArrayList<>();

	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		writtenItems.addAll(chunk.getItems());
	}

	public List<T> getWrittenItems() {
		return this.writtenItems;
	}

}
