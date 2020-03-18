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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class SkipReaderStub<T> extends AbstractExceptionThrowingItemHandlerStub<T> implements ItemReader<T> {

	private T[] items;

	private List<T> read = new ArrayList<>();

	private int counter = -1;

	public SkipReaderStub() throws Exception {
		super();
	}

	@SuppressWarnings("unchecked")
	public SkipReaderStub(T... items) throws Exception {
		super();
		this.items = items;
	}

	@SuppressWarnings("unchecked")
	public void setItems(T... items) {
		Assert.isTrue(counter < 0, "Items cannot be set once reading has started");
		this.items = items;
	}

	public List<T> getRead() {
		return read;
	}

	public void clear() {
		counter = -1;
		read.clear();
	}

	@Nullable
	@Override
	public T read() throws Exception, UnexpectedInputException, ParseException {
		counter++;
		if (counter >= items.length) {
			return null;
		}
		T item = items[counter];
		checkFailure(item);
		read.add(item);
		return item;
	}
}
