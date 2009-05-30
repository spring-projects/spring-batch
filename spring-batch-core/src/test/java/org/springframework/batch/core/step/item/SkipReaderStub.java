/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class SkipReaderStub<T> extends ExceptionThrowingItemHandlerStub<T> implements ItemReader<T> {

	private final T[] items;

	private List<T> read = new ArrayList<T>();

	private int counter = -1;

	public SkipReaderStub(T... items) {
		super();
		this.items = items;
	}

	public SkipReaderStub(T[] items, Collection<T> failures) {
		super(failures);
		this.items = items;
	}

	public SkipReaderStub(T[] items, Collection<T> failures, boolean runtimeException) {
		this(items, failures);
		this.setRuntimeException(runtimeException);
	}

	public List<T> getRead() {
		return read;
	}

	public void clear() {
		read = new ArrayList<T>();
		this.setFailures(new ArrayList<T>());
	}

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
