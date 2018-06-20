/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.json.builder;

import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.json.JsonItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Builder for {@link JsonItemWriter}.
 *
 * @param <T> type of objects to write as Json output.
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JsonItemWriterBuilder<T> {

	private Resource resource;
	private LineAggregator<T> lineAggregator;
	private FlatFileHeaderCallback headerCallback;
	private FlatFileFooterCallback footerCallback;

	private String name;
	private String encoding = JsonItemWriter.DEFAULT_CHARSET;
	private String lineSeparator = JsonItemWriter.DEFAULT_LINE_SEPARATOR;

	private boolean append = false;
	private boolean forceSync = false;
	private boolean saveState = true;
	private boolean shouldDeleteIfExists = true;
	private boolean shouldDeleteIfEmpty = false;
	private boolean transactional = JsonItemWriter.DEFAULT_TRANSACTIONAL;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public JsonItemWriterBuilder<T> saveState(boolean saveState) {
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
	public JsonItemWriterBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush.  Defaults
	 * to false.
	 *
	 * @param forceSync value to set the flag to
	 * @return The current instance of the builder.
	 * @see JsonItemWriter#setForceSync(boolean)
	 */
	public JsonItemWriterBuilder<T> forceSync(boolean forceSync) {
		this.forceSync = forceSync;

		return this;
	}

	/**
	 * String used to separate lines in output.  Defaults to the System property
	 * line.separator.
	 *
	 * @param lineSeparator value to use for a line separator
	 * @return The current instance of the builder.
	 * @see JsonItemWriter#setLineSeparator(String)
	 */
	public JsonItemWriterBuilder<T> lineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;

		return this;
	}

	/**
	 * Set the {@link LineAggregator} to use to aggregate json objects.
	 *
	 * @param lineAggregator to use
	 * @return The current instance of the builder.
	 * @see JsonItemWriter#setLineAggregator(LineAggregator)
	 */
	public JsonItemWriterBuilder<T> lineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;

		return this;
	}

	/**
	 * The {@link Resource} to be used as output.
	 *
	 * @param resource the output of the writer.
	 * @return The current instance of the builder.
	 * @see JsonItemWriter#setResource(Resource)
	 */
	public JsonItemWriterBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * Encoding used for output.
	 *
	 * @param encoding encoding type.
	 * @return The current instance of the builder.
	 * @see JsonItemWriter#setEncoding(String)
	 */
	public JsonItemWriterBuilder<T> encoding(String encoding) {
		this.encoding = encoding;

		return this;
	}

	/**
	 * If set to true, once the step is complete, if the resource previously provdied is
	 * empty, it will be deleted.
	 *
	 * @param shouldDelete defaults to false
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setShouldDeleteIfEmpty(boolean)
	 */
	public JsonItemWriterBuilder<T> shouldDeleteIfEmpty(boolean shouldDelete) {
		this.shouldDeleteIfEmpty = shouldDelete;

		return this;
	}

	/**
	 * If set to true, upon the start of the step, if the resource already exists, it will
	 * be deleted and recreated.
	 *
	 * @param shouldDelete defaults to true
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setShouldDeleteIfExists(boolean)
	 */
	public JsonItemWriterBuilder<T> shouldDeleteIfExists(boolean shouldDelete) {
		this.shouldDeleteIfExists = shouldDelete;

		return this;
	}

	/**
	 * If set to true and the file exists, the output will be appended to the existing
	 * file.
	 *
	 * @param append defaults to false
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setAppendAllowed(boolean)
	 */
	public JsonItemWriterBuilder<T> append(boolean append) {
		this.append = append;

		return this;
	}

	/**
	 * A callback for header processing.
	 *
	 * @param callback {@link FlatFileHeaderCallback} implementation
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setHeaderCallback(FlatFileHeaderCallback)
	 */
	public JsonItemWriterBuilder<T> headerCallback(FlatFileHeaderCallback callback) {
		this.headerCallback = callback;

		return this;
	}

	/**
	 * A callback for footer processing.
	 *
	 * @param callback {@link FlatFileFooterCallback} implementation
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setFooterCallback(FlatFileFooterCallback)
	 */
	public JsonItemWriterBuilder<T> footerCallback(FlatFileFooterCallback callback) {
		this.footerCallback = callback;

		return this;
	}

	/**
	 * If set to true, the flushing of the buffer is delayed while a transaction is active.
	 *
	 * @param transactional defaults to true
	 * @return The current instance of the builder
	 * @see JsonItemWriter#setTransactional(boolean)
	 */
	public JsonItemWriterBuilder<T> transactional(boolean transactional) {
		this.transactional = transactional;

		return this;
	}

	/**
	 * Validate the configuration and build a new {@link JsonItemWriter}.
	 *
	 * @return a new instance of the {@link JsonItemWriter}
	 */
	public JsonItemWriter<T> build() {
		Assert.notNull(this.resource, "A resource is required.");
		Assert.notNull(this.lineAggregator, "A line aggregator is required.");

		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is true");
		}

		JsonItemWriter<T> jsonItemWriter = new JsonItemWriter<>(this.resource, this.lineAggregator);

		jsonItemWriter.setName(this.name);
		jsonItemWriter.setAppendAllowed(this.append);
		jsonItemWriter.setEncoding(this.encoding);
		if (this.headerCallback != null) {
			jsonItemWriter.setHeaderCallback(this.headerCallback);
		}
		if (this.footerCallback != null) {
			jsonItemWriter.setFooterCallback(this.footerCallback);
		}
		jsonItemWriter.setForceSync(this.forceSync);
		jsonItemWriter.setLineSeparator(this.lineSeparator);
		jsonItemWriter.setSaveState(this.saveState);
		jsonItemWriter.setShouldDeleteIfEmpty(this.shouldDeleteIfEmpty);
		jsonItemWriter.setShouldDeleteIfExists(this.shouldDeleteIfExists);
		jsonItemWriter.setTransactional(this.transactional);

		return jsonItemWriter;
	}
}
