/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import org.springframework.batch.infrastructure.item.file.mapping.JsonLineMapper;
import org.springframework.batch.infrastructure.item.json.JsonObjectMarshaller;
import org.springframework.util.Assert;

/**
 * A {@link LineAggregator} implementation that converts objects into
 * <a href="https://jsonlines.org">JSON Lines</a>, using a {@link JsonObjectMarshaller}.
 * This makes the aggregator agnostic to the JSON library: Jackson and Gson
 * implementations of {@link JsonObjectMarshaller} are provided.
 * <p>
 * Each item is aggregated into a single line, so the marshaller must produce a JSON
 * object that fits on one line (that is, it must not pretty-print). This is the case of
 * the default Jackson and Gson marshallers.
 *
 * @param <T> type of the items to aggregate
 * @author Yanming Zhou
 * @author Mahmoud Ben Hassine
 * @since 6.1
 * @see JsonLineMapper
 */
public class JsonLineAggregator<T> implements LineAggregator<T> {

	private final JsonObjectMarshaller<T> jsonObjectMarshaller;

	/**
	 * Create a new {@link JsonLineAggregator} with the provided
	 * {@link JsonObjectMarshaller}.
	 * @param jsonObjectMarshaller the marshaller used to convert an item to a JSON object
	 */
	public JsonLineAggregator(JsonObjectMarshaller<T> jsonObjectMarshaller) {
		Assert.notNull(jsonObjectMarshaller, "The json object marshaller must not be null");
		this.jsonObjectMarshaller = jsonObjectMarshaller;
	}

	/**
	 * Marshal the provided item into JSON string.
	 * @param item value to be converted
	 * @return JSON string
	 */
	@Override
	public String aggregate(T item) {
		return jsonObjectMarshaller.marshal(item);
	}

}
