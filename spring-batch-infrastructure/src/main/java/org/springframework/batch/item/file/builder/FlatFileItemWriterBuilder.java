/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.batch.item.file.builder;

import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link FlatFileItemWriter}
 *
 * @author Michael Minella
 * @since 4.0
 * @see FlatFileItemWriter
 */
public class FlatFileItemWriterBuilder<T> {

	private Resource resource;

	private boolean forceSync = false;

	private String lineSeparator = FlatFileItemWriter.DEFAULT_LINE_SEPARATOR;

	private LineAggregator<T> lineAggregator;

	private String encoding = FlatFileItemWriter.DEFAULT_CHARSET;

	private boolean shouldDeleteIfExists = true;

	private boolean append = false;

	private boolean shouldDeleteIfEmpty = false;

	private boolean saveState = true;

	private FlatFileHeaderCallback headerCallback;

	private FlatFileFooterCallback footerCallback;

	private boolean transactional = FlatFileItemWriter.DEFAULT_TRANSACTIONAL;

	private String name;

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.  Required if
	 * {@link FlatFileItemWriterBuilder#saveState(boolean)} is set to true.
	 *
	 * @param name name of the writer instance
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setName(String)
	 */
	public FlatFileItemWriterBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * The {@link Resource} to be used as output.
	 *
	 * @param resource the output of the writer.
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setResource(Resource)
	 */
	public FlatFileItemWriterBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush.  Defaults
	 * to false.
	 *
	 * @param forceSync value to set the flag to
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setForceSync(boolean)
	 */
	public FlatFileItemWriterBuilder<T> forceSync(boolean forceSync) {
		this.forceSync = forceSync;

		return this;
	}

	/**
	 * String used to separate lines in output.  Defaults to the System property
	 * line.separator.
	 *
	 * @param lineSeparator value to use for a line separator
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setLineSeparator(String)
	 */
	public FlatFileItemWriterBuilder<T> lineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;

		return this;
	}

	/**
	 * Line aggregator used to build the String version of each item.
	 *
	 * @param lineAggregator {@link LineAggregator} implementation
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setLineAggregator(LineAggregator)
	 */
	public FlatFileItemWriterBuilder<T> lineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;

		return this;
	}

	/**
	 * Encoding used for output.
	 *
	 * @param encoding encoding type.
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setEncoding(String)
	 */
	public FlatFileItemWriterBuilder<T> encoding(String encoding) {
		this.encoding = encoding;

		return this;
	}

	/**
	 * If set to true, once the step is complete, if the resource previously provdied is
	 * empty, it will be deleted.
	 *
	 * @param shouldDelete defaults to false
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setShouldDeleteIfEmpty(boolean)
	 */
	public FlatFileItemWriterBuilder<T> shouldDeleteIfEmpty(boolean shouldDelete) {
		this.shouldDeleteIfEmpty = shouldDelete;

		return this;
	}

	/**
	 * If set to true, upon the start of the step, if the resource already exists, it will
	 * be deleted and recreated.
	 *
	 * @param shouldDelete defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setShouldDeleteIfExists(boolean)
	 */
	public FlatFileItemWriterBuilder<T> shouldDeleteIfExists(boolean shouldDelete) {
		this.shouldDeleteIfExists = shouldDelete;

		return this;
	}

	/**
	 * If set to true and the file exists, the output will be appended to the existing
	 * file.
	 *
	 * @param append defaults to false
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setAppendAllowed(boolean)
	 */
	public FlatFileItemWriterBuilder<T> append(boolean append) {
		this.append = append;

		return this;
	}

	/**
	 * If set to false, the state of the output is not maintained and restart is not
	 * supported.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setSaveState(boolean)
	 */
	public FlatFileItemWriterBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * A callback for header processing.
	 *
	 * @param callback {@link FlatFileHeaderCallback} impl
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setHeaderCallback(FlatFileHeaderCallback)
	 */
	public FlatFileItemWriterBuilder<T> headerCallback(FlatFileHeaderCallback callback) {
		this.headerCallback = callback;

		return this;
	}

	/**
	 * A callback for footer processing
	 * @param callback {@link FlatFileFooterCallback} impl
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setFooterCallback(FlatFileFooterCallback)
	 */
	public FlatFileItemWriterBuilder<T> footerCallback(FlatFileFooterCallback callback) {
		this.footerCallback = callback;

		return this;
	}

	/**
	 * If set to true, the flushing of the buffer is delayed while a transaction is active.
	 *
	 * @param transactional defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setTransactional(boolean)
	 */
	public FlatFileItemWriterBuilder<T> transactional(boolean transactional) {
		this.transactional = transactional;

		return this;
	}

	/**
	 * Validates and builds a {@link FlatFileItemWriter}.
	 *
	 * @return a {@link FlatFileItemWriter}
	 */
	public FlatFileItemWriter<T> build() {

		Assert.notNull(this.lineAggregator, "A LineAggregator is required");
		Assert.notNull(this.resource, "A Resource is required");

		if(this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is true");
		}

		FlatFileItemWriter<T> writer = new FlatFileItemWriter<>();

		writer.setName(this.name);
		writer.setAppendAllowed(this.append);
		writer.setEncoding(this.encoding);
		writer.setFooterCallback(this.footerCallback);
		writer.setForceSync(this.forceSync);
		writer.setHeaderCallback(this.headerCallback);
		writer.setLineAggregator(this.lineAggregator);
		writer.setLineSeparator(this.lineSeparator);
		writer.setResource(this.resource);
		writer.setSaveState(this.saveState);
		writer.setShouldDeleteIfEmpty(this.shouldDeleteIfEmpty);
		writer.setShouldDeleteIfExists(this.shouldDeleteIfExists);
		writer.setTransactional(this.transactional);

		return writer;
	}
}
