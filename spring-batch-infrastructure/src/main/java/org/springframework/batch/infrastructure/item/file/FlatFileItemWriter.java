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

package org.springframework.batch.infrastructure.item.file;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.file.transform.LineAggregator;
import org.springframework.batch.infrastructure.item.support.AbstractFileItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This class is an item writer that writes data to a file or stream. The writer also
 * provides restart. The location of the output file is defined by a {@link Resource} and
 * must represent a writable file.<br>
 *
 * Uses buffered writer to improve performance.<br>
 *
 * The implementation is <b>not</b> thread-safe.
 *
 * @author Waseem Malik
 * @author Tomas Slanina
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
// FIXME the design of creating a flat file writer with an optional resource (to support
// the multi-resource case) is broken.
// FIXME The multi-resource writer should create the delegate with the current resource
public class FlatFileItemWriter<T> extends AbstractFileItemWriter<T> {

	protected LineAggregator<T> lineAggregator;

	/**
	 * Create a new {@link FlatFileItemWriter} with the {@link LineAggregator} specified.
	 * @param lineAggregator to use to convert items to lines of text
	 * @since 6.0
	 */
	public FlatFileItemWriter(LineAggregator<T> lineAggregator) {
		Assert.notNull(lineAggregator, "LineAggregator must not be null");
		this.lineAggregator = lineAggregator;
		this.setExecutionContextName(ClassUtils.getShortName(FlatFileItemWriter.class));
	}

	/**
	 * Create a new {@link FlatFileItemWriter} with the {@link WritableResource} and
	 * {@link LineAggregator} specified.
	 * @param resource to write to
	 * @param lineAggregator to use to convert items to lines of text
	 * @since 6.0
	 */
	public FlatFileItemWriter(WritableResource resource, LineAggregator<T> lineAggregator) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.notNull(lineAggregator, "LineAggregator must not be null");
		this.resource = resource;
		this.lineAggregator = lineAggregator;
		this.setExecutionContextName(ClassUtils.getShortName(FlatFileItemWriter.class));
	}

	/**
	 * Assert that mandatory properties (lineAggregator) are set.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (append) {
			shouldDeleteIfExists = false;
		}
	}

	/**
	 * Public setter for the {@link LineAggregator}. This will be used to translate the
	 * item into a line for output.
	 * @param lineAggregator the {@link LineAggregator} to set
	 */
	public void setLineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;
	}

	@Override
	public String doWrite(Chunk<? extends T> items) {
		StringBuilder lines = new StringBuilder();
		for (T item : items) {
			lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
		}
		return lines.toString();
	}

}
