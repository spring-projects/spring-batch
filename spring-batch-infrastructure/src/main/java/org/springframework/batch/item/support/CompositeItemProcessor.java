/*
 * Copyright 2006-2022 the original author or authors.
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Composite {@link ItemProcessor} that passes the item through a sequence of injected
 * <code>ItemTransformer</code>s (return value of previous transformation is the entry
 * value of the next).<br>
 * <br>
 *
 * Note the user is responsible for injecting a chain of {@link ItemProcessor}s that
 * conforms to declared input and output types.
 *
 * @author Robert Kasanicky
 */
public class CompositeItemProcessor<I, O> implements ItemProcessor<I, O>, InitializingBean {

	private @Nullable List<? extends ItemProcessor<?, ?>> delegates;

	/**
	 * Default constructor
	 */
	public CompositeItemProcessor() {

	}

	/**
	 * Convenience constructor for setting the delegates.
	 * @param delegates array of {@link ItemProcessor} delegates that will work on the
	 * item.
	 */
	public CompositeItemProcessor(ItemProcessor<?, ?>... delegates) {
		this(Arrays.asList(delegates));
	}

	/**
	 * Convenience constructor for setting the delegates.
	 * @param delegates list of {@link ItemProcessor} delegates that will work on the
	 * item.
	 */
	public CompositeItemProcessor(List<? extends ItemProcessor<?, ?>> delegates) {
		setDelegates(delegates);
	}

	@Override
	@SuppressWarnings({ "unchecked", "DataFlowIssue" })
	public @Nullable O process(I item) throws Exception {
		Object result = item;

		for (ItemProcessor<?, ?> delegate : delegates) {
			if (result == null) {
				return null;
			}

			result = processItem(delegate, result);
		}
		return (O) result;
	}

	/*
	 * Helper method to work around wildcard capture compiler error: see
	 * https://docs.oracle.com/javase/tutorial/java/generics/capture.html The method
	 * process(capture#1-of ?) in the type ItemProcessor<capture#1-of ?,capture#2-of ?> is
	 * not applicable for the arguments (Object)
	 */
	@SuppressWarnings("unchecked")
	private @Nullable <T> Object processItem(ItemProcessor<T, ?> processor, Object input) throws Exception {
		return processor.process((T) input);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(delegates != null, "The 'delegates' may not be null");
		Assert.state(!delegates.isEmpty(), "The 'delegates' may not be empty");
	}

	/**
	 * Establishes the {@link ItemProcessor} delegates that will work on the item to be
	 * processed.
	 * @param delegates list of {@link ItemProcessor} delegates that will work on the
	 * item.
	 */
	public void setDelegates(List<? extends ItemProcessor<?, ?>> delegates) {
		this.delegates = delegates;
	}

}
