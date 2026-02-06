/*
 * Copyright 2018-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.infrastructure.item.json;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.support.AbstractFileItemWriter;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * Item writer that writes data in json format to an output file. The location of the
 * output file is defined by a {@link WritableResource} and must represent a writable
 * file. Items are transformed to json format using a {@link JsonObjectMarshaller}. Items
 * will be enclosed in a json array as follows:
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
 * The implementation is <b>not</b> thread-safe.
 *
 * @see GsonJsonObjectMarshaller
 * @see JacksonJsonObjectMarshaller
 * @param <T> type of object to write as json representation
 * @author Mahmoud Ben Hassine
 * @author Jimmy Praet
 * @author Yanming Zhou
 * @since 4.1
 */
public class JsonFileItemWriter<T> extends AbstractFileItemWriter<T> {

	private static final char JSON_OBJECT_SEPARATOR = ',';

	private static final char JSON_ARRAY_START = '[';

	private static final char JSON_ARRAY_STOP = ']';

	private JsonObjectMarshaller<T> jsonObjectMarshaller;

	private boolean hasExistingItems;

	/**
	 * Create a new {@link JsonFileItemWriter} instance.
	 * @param resource to write json data to
	 * @param jsonObjectMarshaller used to marshal object into json representation
	 */
	public JsonFileItemWriter(WritableResource resource, JsonObjectMarshaller<T> jsonObjectMarshaller) {
		this.resource = resource;
		Assert.notNull(jsonObjectMarshaller, "json object marshaller must not be null");
		this.jsonObjectMarshaller = jsonObjectMarshaller;
		setHeaderCallback(writer -> writer.write(JSON_ARRAY_START));
		setFooterCallback(writer -> writer.write(this.lineSeparator + JSON_ARRAY_STOP + this.lineSeparator));
	}

	/**
	 * Assert that mandatory properties (jsonObjectMarshaller) are set.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.append) {
			this.shouldDeleteIfExists = false;
		}
	}

	/**
	 * Set the {@link JsonObjectMarshaller} to use to marshal object to json.
	 * @param jsonObjectMarshaller the marshaller to use
	 */
	public void setJsonObjectMarshaller(JsonObjectMarshaller<T> jsonObjectMarshaller) {
		this.jsonObjectMarshaller = jsonObjectMarshaller;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		try {
			if (this.append && this.resource != null && this.resource.exists() && this.resource.contentLength() > 0) {
				this.hasExistingItems = reopen(this.resource.getFile());
			}
		}
		catch (IOException ex) {
			throw new ItemStreamException(ex.getMessage(), ex);
		}
		super.open(executionContext);
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public String doWrite(Chunk<? extends T> items) {
		StringBuilder lines = new StringBuilder();
		if (this.hasExistingItems) {
			lines.append(JSON_OBJECT_SEPARATOR).append(this.lineSeparator);
			this.hasExistingItems = false;
		}
		Iterator<? extends T> iterator = items.iterator();
		if (!items.isEmpty() && state.getLinesWritten() > 0) {
			lines.append(JSON_OBJECT_SEPARATOR).append(this.lineSeparator);
		}
		while (iterator.hasNext()) {
			T item = iterator.next();
			lines.append(' ').append(this.jsonObjectMarshaller.marshal(item));
			if (iterator.hasNext()) {
				lines.append(JSON_OBJECT_SEPARATOR).append(this.lineSeparator);
			}
		}
		return lines.toString();
	}

	private boolean reopen(File file) throws IOException {
		long length = file.length();
		// try to delete lineSeparator + JSON_ARRAY_STOP + lineSeparator
		long pos = length - (1 + 2L * this.lineSeparator.length());
		if (pos <= 0) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			raf.setLength(pos);
			// file content is not empty or empty JSON array
			// (JSON_ARRAY_START + 2 * lineSeparator + JSON_ARRAY_STOP + lineSeparator)
			return length > 2 + 3L * this.lineSeparator.length();
		}
	}

}
