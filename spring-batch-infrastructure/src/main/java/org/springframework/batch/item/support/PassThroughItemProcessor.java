/*
 * Copyright 2006-2019 the original author or authors.
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

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

/**
 * Simple {@link ItemProcessor} that does nothing - simply passes its argument
 * through to the caller. Useful as a default when the reader and writer in a
 * business process deal with items of the same type, and no transformations are
 * required.
 * 
 * @author Dave Syer
 * 
 */
public class PassThroughItemProcessor<T> implements ItemProcessor<T, T> {

	/**
	 * Just returns the item back to the caller.
	 * 
	 * @return the item
	 * @see ItemProcessor#process(Object)
	 */
    @Nullable
	@Override
	public T process(T item) throws Exception {
		return item;
	}

}
