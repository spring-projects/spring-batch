/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.support;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ItemWriter;

/**
 * Calls a collection of ItemWriters in fixed-order sequence.
 * 
 * The implementation is thread-safe if all delegates are thread-safe.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class CompositeItemWriter<T> implements ItemWriter<T> {

	private List<ItemWriter<? super T>> delegates;

	public void setDelegates(ItemWriter<? super T>[] delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(List<? extends  T> item) throws Exception {
		for (ItemWriter<? super T> writer : delegates) {
			writer.write(item);
		}
	}

}
