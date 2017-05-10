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

package org.springframework.batch.item.ldif.builder;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ldif.MappingLdifReader;
import org.springframework.batch.item.ldif.RecordCallbackHandler;
import org.springframework.batch.item.ldif.RecordMapper;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified MappingLdifReader.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class MappingLdifReaderBuilder<T> {
	private Resource resource;

	private int recordsToSkip = 0;

	private boolean strict = true;

	private RecordCallbackHandler skippedRecordsCallback;

	private boolean saveState = true;

	private int currentItemCount = 0;

	private int maxItemCount = Integer.MAX_VALUE;

	private RecordMapper<T> recordMapper;

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link MappingLdifReader#open(org.springframework.batch.item.ExecutionContext)} if
	 * the input resource does not exist.
	 *
	 * @param strict true by default
	 * @return this instance for method chaining.
	 * @see MappingLdifReader#setStrict(boolean)
	 */
	public MappingLdifReaderBuilder<T> strict(boolean strict) {
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
	 * @see MappingLdifReader#setSkippedRecordsCallback(RecordCallbackHandler)
	 */
	public MappingLdifReaderBuilder<T> skippedRecordsCallback(RecordCallbackHandler skippedRecordsCallback) {
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
	 * @see MappingLdifReader#setRecordsToSkip(int)
	 */
	public MappingLdifReaderBuilder<T> recordsToSkip(int recordsToSkip) {
		this.recordsToSkip = recordsToSkip;

		return this;
	}

	/**
	 * Establishes the resource that will be used as the input for the MappingLdifReader.
	 *
	 * @param resource the resource that will be read.
	 * @return this instance for method chaining.
	 * @see MappingLdifReader#setResource(Resource)
	 */
	public MappingLdifReaderBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to save any
	 * state from this stream, and you don't need it to be restartable. Always set it to
	 * false if the reader is being used in a concurrent environment.
	 *
	 * @param saveState flag value (default true).
	 * @return this instance for method chaining.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setSaveState(boolean)
	 */
	public MappingLdifReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The index of the item to start reading from. If the {@link ExecutionContext}
	 * contains a key <code>[name].read.count</code> (where <code>[name]</code> is the
	 * name of this component) the value from the {@link ExecutionContext} will be used in
	 * preference.
	 *
	 * @param count the value of the current item count.
	 * @return this instance for method chaining.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public MappingLdifReaderBuilder<T> currentItemCount(int count) {
		this.currentItemCount = count;

		return this;
	}

	/**
	 * The maximum index of the items to be read. If the {@link ExecutionContext} contains
	 * a key <code>[name].read.count.max</code> (where <code>[name]</code> is the name of
	 * this component) the value from the {@link ExecutionContext} will be used in
	 * preference.
	 *
	 * @param count the value of the maximum item count.
	 * @return this instance for method chaining.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 *
	 */
	public MappingLdifReaderBuilder<T> maxItemCount(int count) {
		this.maxItemCount = count;

		return this;
	}

	/**
	 * Setter for object mapper. This property is required to be set.
	 * @param recordMapper maps record to an object
	 */
	public MappingLdifReaderBuilder<T> recordMapper(RecordMapper<T> recordMapper) {
		this.recordMapper = recordMapper;

		return this;
	}

	/**
	 * Returns a fully constructed {@link MappingLdifReader}.
	 *
	 * @return a new {@link org.springframework.batch.item.ldif.MappingLdifReader}
	 */
	public MappingLdifReader<T> build() {
		Assert.notNull(this.resource, "Resource is required.");
		Assert.notNull(this.recordMapper, "RecordMapper is required.");

		MappingLdifReader reader = new MappingLdifReader();
		reader.setResource(this.resource);
		reader.setRecordsToSkip(this.recordsToSkip);
		reader.setSaveState(saveState);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setRecordMapper(this.recordMapper);
		if (this.skippedRecordsCallback != null) {
			reader.setSkippedRecordsCallback(this.skippedRecordsCallback);
		}
		reader.setStrict(this.strict);

		return reader;
	}
}
