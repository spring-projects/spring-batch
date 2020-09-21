/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.batch.item.json;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemStreamReader} implementation that reads Json objects from a
 * {@link Resource} having the following format:
 * <p>
 * <code>
 * [
 *    {
 *       // JSON object
 *    },
 *    {
 *       // JSON object
 *    }
 * ]
 * </code>
 * <p>
 *
 * The implementation is <b>not</b> thread-safe.
 *
 * @param <T> the type of json objects to read
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JsonItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements
		ResourceAwareItemReaderItemStream<T> {

	private static final Log LOGGER = LogFactory.getLog(JsonItemReader.class);

	private Resource resource;

	private JsonObjectReader<T> jsonObjectReader;

	private boolean strict = true;

	/**
	 * Create a new {@link JsonItemReader} instance.
	 * @param resource the input json resource
	 * @param jsonObjectReader the json object reader to use
	 */
	public JsonItemReader(Resource resource, JsonObjectReader<T> jsonObjectReader) {
		Assert.notNull(resource, "The resource must not be null.");
		Assert.notNull(jsonObjectReader, "The json object reader must not be null.");
		this.resource = resource;
		this.jsonObjectReader = jsonObjectReader;
		setExecutionContextName(ClassUtils.getShortName(JsonItemReader.class));
	}

	/**
	 * Create a new {@link JsonItemReader} instance.
	 */
	public JsonItemReader(){
		setExecutionContextName(ClassUtils.getShortName(JsonItemReader.class));
	}

	/**
	 * Set the {@link JsonObjectReader} to use to read and map Json fragments to domain objects.
	 * @param jsonObjectReader the json object reader to use
	 */
	public void setJsonObjectReader(JsonObjectReader<T> jsonObjectReader) {
		this.jsonObjectReader = jsonObjectReader;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(org.springframework.batch.item.ExecutionContext)} if the
	 * input resource does not exist.
	 * @param strict true by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	@Override
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	@Nullable
	@Override
	protected T doRead() throws Exception {
		return jsonObjectReader.read();
	}

	@Override
	protected void doOpen() throws Exception {
		Assert.notNull(this.resource, "The resource must not be null.");
		Assert.notNull(this.jsonObjectReader, "The json object reader must not be null.");
		if (!this.resource.exists()) {
			if (this.strict) {
				throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode)");
			}
			LOGGER.warn("Input resource does not exist " + this.resource.getDescription());
			return;
		}
		if (!this.resource.isReadable()) {
			if (this.strict) {
				throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode)");
			}
			LOGGER.warn("Input resource is not readable " + this.resource.getDescription());
			return;
		}
		this.jsonObjectReader.open(this.resource);
	}

	@Override
	protected void doClose() throws Exception {
		this.jsonObjectReader.close();
	}

}
