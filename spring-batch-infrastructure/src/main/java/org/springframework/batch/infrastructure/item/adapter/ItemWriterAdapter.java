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

package org.springframework.batch.infrastructure.item.adapter;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * Delegates item processing to a custom method - passes the item as an argument for the
 * delegate method.
 *
 * <p>
 * This adapter is thread-safe as long as the delegate <code>ItemWriter</code> is
 * thread-safe.
 * </p>
 *
 * @see PropertyExtractingDelegatingItemWriter
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class ItemWriterAdapter<T> extends AbstractMethodInvokingDelegator<T> implements ItemWriter<T> {

	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		for (T item : items) {
			invokeDelegateMethodWithArgument(item);
		}
	}

}
