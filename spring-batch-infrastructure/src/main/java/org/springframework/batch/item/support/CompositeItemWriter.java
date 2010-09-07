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

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Calls a collection of {@link ItemWriter}s in fixed-order sequence.<br/>
 * <br/>
 * 
 * The implementation is thread-safe if all delegates are thread-safe.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class CompositeItemWriter<T> implements ItemStreamWriter<T>, InitializingBean {

	private List<ItemWriter<? super T>> delegates;

	private boolean ignoreItemStream = false;

	public void setIgnoreItemStream(boolean ignoreItemStream) {
		this.ignoreItemStream = ignoreItemStream;
	}

	public void write(List<? extends T> item) throws Exception {
		for (ItemWriter<? super T> writer : delegates) {
			writer.write(item);
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegates, "The 'delgates' may not be null");
		Assert.notEmpty(delegates, "The 'delgates' may not be empty");
	}

	public void setDelegates(List<ItemWriter<? super T>> delegates) {
		this.delegates = delegates;
	}

	public void close() throws ItemStreamException {
		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream)) {
				((ItemStream) writer).close();
			}
		}
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream)) {
				((ItemStream) writer).open(executionContext);
			}
		}
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream)) {
				((ItemStream) writer).update(executionContext);
			}
		}
	}

}
