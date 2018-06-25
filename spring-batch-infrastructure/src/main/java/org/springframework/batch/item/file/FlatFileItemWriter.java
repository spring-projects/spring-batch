/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.item.file;

import java.io.IOException;
import java.util.List;

import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This class is an item writer that writes data to a file or stream. The writer
 * also provides restart. The location of the output file is defined by a
 * {@link Resource} and must represent a writable file.<br>
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
 */
public class FlatFileItemWriter<T> extends AbstractFileItemWriter<T> {

	protected LineAggregator<T> lineAggregator;

	public FlatFileItemWriter() {
		this.setExecutionContextName(ClassUtils.getShortName(FlatFileItemWriter.class));
	}

	/**
	 * Assert that mandatory properties (lineAggregator) are set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(lineAggregator, "A LineAggregator must be provided.");
		if (append) {
			shouldDeleteIfExists = false;
		}
	}

	/**
	 * Public setter for the {@link LineAggregator}. This will be used to
	 * translate the item into a line for output.
	 * 
	 * @param lineAggregator the {@link LineAggregator} to set
	 */
	public void setLineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;
	}

	/**
	 * Writes out a string followed by a "new line", where the format of the new
	 * line separator is determined by the underlying operating system. If the
	 * input is not a String and a converter is available the converter will be
	 * applied and then this method recursively called with the result. If the
	 * input is an array or collection each value will be written to a separate
	 * line (recursively calling this method for each value). If no converter is
	 * supplied the input object's toString method will be used.<br>
	 * 
	 * @param items list of items to be written to output stream
	 * @throws Exception if the transformer or file output fail,
	 * WriterNotOpenException if the writer has not been initialized.
	 */
	@Override
	public void write(List<? extends T> items) throws Exception {

		if (!getOutputState().isInitialized()) {
			throw new WriterNotOpenException("Writer must be open before it can be written to");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to flat file with " + items.size() + " items.");
		}

		OutputState state = getOutputState();

		StringBuilder lines = new StringBuilder();
		int lineCount = 0;
		for (T item : items) {
			lines.append(lineAggregator.aggregate(item) + lineSeparator);
			lineCount++;
		}
		try {
			state.write(lines.toString());
		}
		catch (IOException e) {
			throw new WriteFailedException("Could not write data.  The file may be corrupt.", e);
		}
		state.setLinesWritten(state.getLinesWritten() + lineCount);
	}

}
