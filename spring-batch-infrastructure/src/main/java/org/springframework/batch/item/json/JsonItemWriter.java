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

package org.springframework.batch.item.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Item writer that writes data in json format to an output file.
 * Items are transformed to json format using a {@link LineAggregator}.
 * Items will be enclosed in a json array as follows:
 *
 * <p>
 * <code>
 * [
 *  {json object},
 *  {json object},
 *  {json object}
 * ]
 * </code>
 * </p>
 *
 * @see GsonLineAggregator
 * @see JacksonLineAggregator
 * @param <T> type of object to write as json representation
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JsonItemWriter<T> extends FlatFileItemWriter<T> {

	private static final char JSON_OBJECT_SEPARATOR = ',';
	private static final char JSON_ARRAY_START = '[';
	private static final char JSON_ARRAY_STOP = ']';

	/**
	 * Create a new {@link JsonItemWriter} instance.
	 * @param resource to write json data to
	 * @param lineAggregator used to aggregate object into json representation
	 */
	public JsonItemWriter(Resource resource, LineAggregator<T> lineAggregator) {
		Assert.notNull(resource, "resource must not be null");
		Assert.notNull(lineAggregator, "line aggregator must not be null");
		setResource(resource);
		setLineAggregator(lineAggregator);
		setHeaderCallback(writer -> writer.write(JSON_ARRAY_START));
		setFooterCallback(writer -> writer.write(this.lineSeparator + JSON_ARRAY_STOP + this.lineSeparator));
	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		if (!getOutputState().isInitialized()) {
			throw new WriterNotOpenException("Writer must be open before it can be written to");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to json file with " + items.size() + " items.");
		}

		OutputState state = getOutputState();

		StringBuilder lines = new StringBuilder();
		int lineCount = 0;
		Iterator<? extends T> iterator = items.iterator();
		while (iterator.hasNext()) {
			if (iterator.hasNext() && lineCount == 0 && state.getLinesWritten() > 0) {
				lines.append(JSON_OBJECT_SEPARATOR).append(this.lineSeparator);
			}
			T item = iterator.next();
			lines.append(' ').append(this.lineAggregator.aggregate(item));
			if (iterator.hasNext()) {
				lines.append(JSON_OBJECT_SEPARATOR).append(this.lineSeparator);
			}
			lineCount++;
		}
		try {
			state.write(lines.toString());
		}
		catch (IOException e) {
			throw new WriteFailedException("Could not write data. The file may be corrupt.", e);
		}
		state.setLinesWritten(state.getLinesWritten() + lineCount);
	}

}
