/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.batch.item.support.builder;

import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified {@link CompositeItemProcessorBuilder}.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class CompositeItemProcessorBuilder<I, O> {
	private List<? extends ItemProcessor<?, ?>> delegates;

	/**
	 * Establishes the {@link ItemProcessor} delegates that will work on the item to be processed.
	 * @param delegates list of {@link ItemProcessor} delegates that will work on the item.
	 * @return this instance for method chaining.
	 * @see CompositeItemProcessor#setDelegates(List)
	 */
	public CompositeItemProcessorBuilder<I, O> delegates(List<? extends ItemProcessor<?, ?>> delegates) {
		this.delegates = delegates;

		return this;
	}

	/**
	 * Returns a fully constructed {@link CompositeItemProcessor}.
	 *
	 * @return a new {@link CompositeItemProcessor}
	 */
	public CompositeItemProcessor<I, O> build() {
		Assert.notNull(delegates, "A list of delegates is required.");
		Assert.notEmpty(delegates, "The delegates list must have one or more delegates.");

		CompositeItemProcessor<I, O> processor = new CompositeItemProcessor<>();
		processor.setDelegates(this.delegates);
		return processor;
	}
}
