/*
 * Copyright 2018 the original author or authors.
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

import com.google.gson.Gson;

/**
 * A json object marshaller that uses <a href="https://github.com/google/gson">Google Gson</a>
 * to marshal an object into a json representation.
 *
 * @param <T> type of objects to marshal
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class GsonJsonObjectMarshaller<T> implements JsonObjectMarshaller<T> {

	private Gson gson = new Gson();

	/**
	 * Set the {@link Gson} object to use.
	 * @param gson object to use
	 */
	public void setGson(Gson gson) {
		this.gson = gson;
	}

	@Override
	public String marshal(T item) {
		return gson.toJson(item);
	}
}
