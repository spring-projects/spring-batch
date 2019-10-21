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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

/**
 * Simple {@link ItemStream} that delegates to a list of other streams.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
public class CompositeItemStream implements ItemStream {

	private final List<ItemStream> streams = new ArrayList<>();

	/**
	 * Public setter for the {@link ItemStream}s.
	 * 
	 * @param streams array of {@link ItemStream}.
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams.addAll(Arrays.asList(streams));
	}

	/**
	 * Register a {@link ItemStream} as one of the interesting providers under
	 * the provided key.
	 *
	 * @param stream an instance of {@link ItemStream} to be added to the list of streams.
	 */
	public void register(ItemStream stream) {
		synchronized (streams) {
			if (!streams.contains(stream)) {
				streams.add(stream);
			}
		}
	}

	/**
	 * 
	 */
	public CompositeItemStream() {
		super();
	}

	/**
	 * Simple aggregate {@link ExecutionContext} provider for the contributions
	 * registered under the given key.
	 * 
	 * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
	 */
    @Override
	public void update(ExecutionContext executionContext) {
		for (ItemStream itemStream : streams) {
			itemStream.update(executionContext);
		}
	}

	/**
	 * Broadcast the call to close.

	 * @throws ItemStreamException thrown if one of the {@link ItemStream}s in
	 * the list fails to close.  This is a sequential operation so all itemStreams
	 * in the list after the one that failed to close will remain open.
	 */
    @Override
	public void close() throws ItemStreamException {
		for (ItemStream itemStream : streams) {
			itemStream.close();
		}
	}

	/**
	 * Broadcast the call to open.
	 *
	 * @throws ItemStreamException thrown if one of the {@link ItemStream}s in
	 * the list fails to open.  This is a sequential operation so all itemStreams
	 * in the list after the one that failed to open will not be opened.
	 */
    @Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemStream itemStream : streams) {
			itemStream.open(executionContext);
		}
	}

}
