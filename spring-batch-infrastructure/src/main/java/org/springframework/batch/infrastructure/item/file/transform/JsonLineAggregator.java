/*
 * Copyright 2006-2026 the original author or authors.
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

/**
 * A {@link LineAggregator} implementation that converts objects into
 * <a href="https://jsonlines.org">JSON Lines</a>.
 *
 * @author Yanming Zhou
 * @see JsonLineMapper
 */
public class JsonLineAggregator<T> implements LineAggregator<T> {

	private final JsonObjectMarshaller<T> jsonObjectMarshaller;

	public JsonLineAggregator(JsonObjectMarshaller<T> jsonObjectMarshaller) {
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
