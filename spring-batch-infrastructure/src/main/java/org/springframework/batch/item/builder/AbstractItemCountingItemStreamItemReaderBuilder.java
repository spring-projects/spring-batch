/*
 * Copyright 2017 the original author or authors.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.builder;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

/**
 * Abstract superclass for builders that create streams that support restart by storing
 * item count in the {@link ExecutionContext} (therefore requires item ordering to be
 * preserved between runs).
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public abstract class AbstractItemCountingItemStreamItemReaderBuilder<T> extends AbstractItemStreamSupportBuilder<T> {

	protected int currentItemCount = 0;

	protected int maxItemCount = Integer.MAX_VALUE;

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public T maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
		return (T) this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public T currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return (T) this;
	}

}
