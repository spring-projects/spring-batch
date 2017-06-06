/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.support.builder;

import org.apache.shiro.util.Assert;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.support.CompositeItemStream;

/**
 * Creates a fully qualified CompositeItemStream.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class CompositeItemStreamBuilder {
	private ItemStream[] streams;

	/**
	 * Establish the streams to be used.
	 *
	 * @param streams an array of ItemStream representing the streams to be used.
	 * @return this instance for method chaining.
	 * @see org.springframework.batch.item.support.CompositeItemStream#setStreams(ItemStream[])
	 */
	public CompositeItemStreamBuilder streams(ItemStream[] streams) {
		Assert.notNull(streams, "Streams must not be null.");
		this.streams = streams;

		return this;
	}

	/**
	 * Returns a fully constructed {@link CompositeItemStream}.
	 *
	 * @return a new {@link CompositeItemStream}
	 */
	public CompositeItemStream build() {
		CompositeItemStream stream = new CompositeItemStream();
		if (streams != null) {
			stream.setStreams(this.streams);
		}
		return stream;
	}
}
