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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareProxyFactory;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0.1
 */
public class SkipWriterStub<T> extends AbstractExceptionThrowingItemHandlerStub<T> implements ItemWriter<T> {

	private final List<T> written = new ArrayList<>();

	private final List<T> committed = TransactionAwareProxyFactory.createTransactionalList();

	public SkipWriterStub() throws Exception {
		super();
	}

	public List<T> getWritten() {
		return written;
	}

	public List<T> getCommitted() {
		return committed;
	}

	public void clear() {
		written.clear();
		committed.clear();
	}

	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		logger.debug("Writing: " + items);
		for (T item : items) {
			written.add(item);
			committed.add(item);
			checkFailure(item);
		}
	}

}
