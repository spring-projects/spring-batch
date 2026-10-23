/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;

/**
 * Simple {@link ItemStream} that delegates to a list of other streams.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Elimelec Burghelea
 */
public class CompositeItemStream implements ItemStream {

	private static final Log logger = LogFactory.getLog(CompositeItemStream.class);

	private final List<ItemStream> streams = new ArrayList<>();

	/**
	 * Public setter for the {@link ItemStream}s.
	 * @param streams {@link List} of {@link ItemStream}.
	 */
	public void setStreams(List<ItemStream> streams) {
		streams.forEach(this::register);
	}

	/**
	 * Public setter for the {@link ItemStream}s.
	 * @param streams array of {@link ItemStream}.
	 */
	public void setStreams(ItemStream[] streams) {
		setStreams(Arrays.asList(streams));
	}

	/**
	 * Register a {@link ItemStream} as one of the interesting providers under the
	 * provided key.
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
	 * Best-effort check for two registered {@link ItemStreamSupport}s sharing the same
	 * {@link ItemStreamSupport#getName name}: both would persist their restart state
	 * under the same {@link ExecutionContext} key namespace and silently overwrite each
	 * other's position. This can't tell whether either stream actually persists state
	 * (not every {@link ItemStreamSupport} exposes {@code saveState}), so it only warns.
	 * <p>
	 * Deliberately run from {@link #open(ExecutionContext)} rather than
	 * {@link #register(ItemStream)}: a stream registered while still {@code @StepScope}d
	 * (the common case for readers/writers) cannot have {@code getName()} called on it
	 * before the step context is registered, which {@link #register(ItemStream)} may run
	 * ahead of.
	 */
	private void warnOnDuplicateNames() {
		List<ItemStreamSupport> named = new ArrayList<>();
		for (ItemStream stream : this.streams) {
			if (!(stream instanceof ItemStreamSupport itemStreamSupport)) {
				continue;
			}
			String name = itemStreamSupport.getName();
			if (name == null) {
				continue;
			}
			for (ItemStreamSupport other : named) {
				if (name.equals(other.getName())) {
					logger.warn("Duplicate ExecutionContext name '" + name + "' between " + other.getClass().getName()
							+ " and " + itemStreamSupport.getClass().getName()
							+ ". If both persist state (saveState=true), one will silently overwrite the other's "
							+ "restart position. Give each stream an explicit, unique name.");
				}
			}
			named.add(itemStreamSupport);
		}
	}

	/**
	 * Default constructor
	 */
	public CompositeItemStream() {
		super();
	}

	/**
	 * Convenience constructor for setting the {@link ItemStream}s.
	 * @param streams {@link List} of {@link ItemStream}.
	 */
	public CompositeItemStream(List<ItemStream> streams) {
		setStreams(streams);
	}

	/**
	 * Convenience constructor for setting the {@link ItemStream}s.
	 * @param streams array of {@link ItemStream}.
	 */
	public CompositeItemStream(ItemStream... streams) {
		setStreams(streams);
	}

	/**
	 * Simple aggregate {@link ExecutionContext} provider for the contributions registered
	 * under the given key.
	 *
	 * @see ItemStream#update(ExecutionContext)
	 */
	@Override
	public void update(ExecutionContext executionContext) {
		for (ItemStream itemStream : streams) {
			itemStream.update(executionContext);
		}
	}

	/**
	 * Broadcast the call to close.
	 * @throws ItemStreamException thrown if one of the {@link ItemStream}s in the list
	 * fails to close. Original exceptions thrown by delegates are added as suppressed
	 * exceptions into this one, in the same order as delegates were registered.
	 */
	@Override
	public void close() throws ItemStreamException {
		List<Exception> exceptions = new ArrayList<>();

		for (ItemStream itemStream : streams) {
			try {
				itemStream.close();
			}
			catch (Exception e) {
				exceptions.add(e);
			}
		}

		if (!exceptions.isEmpty()) {
			String message = String.format("Failed to close %d delegate(s) due to exceptions", exceptions.size());
			ItemStreamException holder = new ItemStreamException(message);
			exceptions.forEach(holder::addSuppressed);
			throw holder;
		}
	}

	/**
	 * Broadcast the call to open.
	 * @throws ItemStreamException thrown if one of the {@link ItemStream}s in the list
	 * fails to open. This is a sequential operation so all itemStreams in the list after
	 * the one that failed to open will not be opened.
	 */
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		warnOnDuplicateNames();
		for (ItemStream itemStream : streams) {
			itemStream.open(executionContext);
		}
	}

}
