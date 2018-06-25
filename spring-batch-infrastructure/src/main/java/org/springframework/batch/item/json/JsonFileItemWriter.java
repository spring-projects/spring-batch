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
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Item writer that writes data in json format to an output file. The location
 * of the output file is defined by a {@link Resource} and must represent a
 * writable file. Items are transformed to json format using a
 * {@link JsonObjectMarshaller}. Items will be enclosed in a json array as follows:
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
 * @since 4.1
 */
public class JsonFileItemWriter<T> extends AbstractFileItemWriter<T> {

	private static final char JSON_OBJECT_SEPARATOR = ',';
	private static final char JSON_ARRAY_START = '[';
	private static final char JSON_ARRAY_STOP = ']';

	private JsonObjectMarshaller<T> jsonObjectMarshaller;

	/**
	 * Create a new {@link JsonFileItemWriter} instance.
	 * @param resource to write json data to
	 * @param jsonObjectMarshaller used to marshal object into json representation
	 */
	public JsonFileItemWriter(Resource resource, JsonObjectMarshaller<T> jsonObjectMarshaller) {
		Assert.notNull(resource, "resource must not be null");
		Assert.notNull(jsonObjectMarshaller, "json object marshaller must not be null");
		setResource(resource);
		setJsonObjectMarshaller(jsonObjectMarshaller);
		setHeaderCallback(writer -> writer.write(JSON_ARRAY_START));
		setFooterCallback(writer -> writer.write(this.lineSeparator + JSON_ARRAY_STOP + this.lineSeparator));
		setExecutionContextName(ClassUtils.getShortName(JsonFileItemWriter.class));
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
			lines.append(' ').append(this.jsonObjectMarshaller.marshal(item));
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
