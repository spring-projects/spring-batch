/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.ldif.builder;

import org.springframework.batch.item.ldif.LdifReader;
import org.springframework.batch.item.ldif.RecordCallbackHandler;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified LdifReader.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class LdifReaderBuilder  {
	private Resource resource;

	private int recordsToSkip = 0;

	private boolean strict = true;

	private RecordCallbackHandler skippedRecordsCallback;

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public LdifReaderBuilder saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public LdifReaderBuilder name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public LdifReaderBuilder maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public LdifReaderBuilder currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link LdifReader#open(org.springframework.batch.item.ExecutionContext)} if the
	 * input resource does not exist.
	 *
	 * @param strict true by default
	 * @return this instance for method chaining.
	 * @see LdifReader#setStrict(boolean)
	 */
	public LdifReaderBuilder strict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * {@link RecordCallbackHandler RecordCallbackHandler} implementations can be used to
	 * take action on skipped records.
	 *
	 * @param skippedRecordsCallback will be called for each one of the initial skipped
	 * lines before any items are read.
	 * @return this instance for method chaining.
	 * @see LdifReader#setSkippedRecordsCallback(RecordCallbackHandler)
	 */
	public LdifReaderBuilder skippedRecordsCallback(RecordCallbackHandler skippedRecordsCallback) {
		this.skippedRecordsCallback = skippedRecordsCallback;

		return this;
	}

	/**
	 * Public setter for the number of lines to skip at the start of a file. Can be used
	 * if the file contains a header without useful (column name) information, and without
	 * a comment delimiter at the beginning of the lines.
	 *
	 * @param recordsToSkip the number of lines to skip
	 * @return this instance for method chaining.
	 * @see LdifReader#setRecordsToSkip(int)
	 */
	public LdifReaderBuilder recordsToSkip(int recordsToSkip) {
		this.recordsToSkip = recordsToSkip;

		return this;
	}

	/**
	 * Establishes the resource that will be used as the input for the LdifReader.
	 *
	 * @param resource the resource that will be read.
	 * @return this instance for method chaining.
	 * @see LdifReader#setResource(Resource)
	 */
	public LdifReaderBuilder resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * Returns a fully constructed {@link LdifReader}.
	 *
	 * @return a new {@link org.springframework.batch.item.ldif.LdifReader}
	 */
	public LdifReader build() {
		Assert.notNull(this.resource, "Resource is required.");
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		LdifReader reader = new LdifReader();
		reader.setResource(this.resource);
		reader.setRecordsToSkip(this.recordsToSkip);
		reader.setSaveState(this.saveState);
		reader.setName(this.name);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		if (this.skippedRecordsCallback != null) {
			reader.setSkippedRecordsCallback(this.skippedRecordsCallback);
		}
		reader.setStrict(this.strict);

		return reader;
	}

}
