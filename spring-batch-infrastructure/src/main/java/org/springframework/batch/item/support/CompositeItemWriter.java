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

import java.util.ArrayList;
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

	private final List<ItemWriter<? super T>> delegates = new ArrayList<ItemWriter<? super T>>();

	private boolean ignoreItemStream = false;

	public void setIgnoreItemStream(boolean ignoreItemStream) {
		this.ignoreItemStream = ignoreItemStream;
	}

    @Override
	public void write(List<? extends T> item) throws Exception {
                synchronized (delegates) {
                    for (ItemWriter<? super T> writer : delegates) {
                            writer.write(item);
                    }
                }
	}

    @Override
	public void afterPropertiesSet() throws Exception {
                synchronized (delegates) {
                    Assert.notEmpty(delegates, "The 'delgates' may not be empty");
                }
	}

	public void setDelegates(List<ItemWriter<? super T>> delegates) {
                synchronized (delegates) {
                    delegates.clear();
                    delegates.addAll(delegates);
                }
	}

    @Override
	public void close() throws ItemStreamException {
                synchronized (delegates) {
                    for (ItemWriter<? super T> writer : delegates) {
                            if (!ignoreItemStream && (writer instanceof ItemStream)) {
                                    ((ItemStream) writer).close();
                            }
                    }
                }
	}

    @Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
                synchronized (delegates) {
                    for (ItemWriter<? super T> writer : delegates) {
                            if (!ignoreItemStream && (writer instanceof ItemStream)) {
                                    ((ItemStream) writer).open(executionContext);
                            }
                    }
                }
	}

    @Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
                synchronized (delegates) {
                    for (ItemWriter<? super T> writer : delegates) {
                            if (!ignoreItemStream && (writer instanceof ItemStream)) {
                                    ((ItemStream) writer).update(executionContext);
                            }
                    }
                }
	}

}
