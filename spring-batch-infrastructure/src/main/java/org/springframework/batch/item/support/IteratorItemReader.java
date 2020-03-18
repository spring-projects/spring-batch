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

import java.util.Iterator;

import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} that pulls data from a {@link Iterator} or
 * {@link Iterable} using the constructors.
 * 
 * @author Juliusz Brzostek
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class IteratorItemReader<T> implements ItemReader<T> {

	/**
	 * Internal iterator
	 */
	private final Iterator<T> iterator;

	/**
	 * Construct a new reader from this iterable (could be a collection), by
	 * extracting an instance of {@link Iterator} from it.
	 * 
	 * @param iterable in instance of {@link Iterable}
	 * 
	 * @see Iterable#iterator()
	 */
	public IteratorItemReader(Iterable<T> iterable) {
		Assert.notNull(iterable, "Iterable argument cannot be null!");
		this.iterator = iterable.iterator();
	}

	/**
	 * Construct a new reader from this iterator directly.
	 * @param iterator an instance of {@link Iterator}
	 */
	public IteratorItemReader(Iterator<T> iterator) {
		Assert.notNull(iterator, "Iterator argument cannot be null!");
		this.iterator = iterator;
	}

	/**
	 * Implementation of {@link ItemReader#read()} that just iterates over the
	 * iterator provided.
	 */
    @Nullable
	@Override
	public T read() {
		if (iterator.hasNext())
			return iterator.next();
		else
			return null; // end of data
	}

}
