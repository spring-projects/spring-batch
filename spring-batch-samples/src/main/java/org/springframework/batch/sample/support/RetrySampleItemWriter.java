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

package org.springframework.batch.sample.support;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * Simulates temporary output trouble - requires to retry 3 times to pass successfully.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class RetrySampleItemWriter<T> implements ItemWriter<T> {

	private int counter = 0;

	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		int current = counter;
		counter += items.size();
		if (current < 3 && (counter >= 2 || counter >= 3)) {
			throw new IllegalStateException("Temporary error");
		}
	}

	/**
	 * @return number of times {@link #write(Chunk)} method was called.
	 */
	public int getCounter() {
		return counter;
	}

}
