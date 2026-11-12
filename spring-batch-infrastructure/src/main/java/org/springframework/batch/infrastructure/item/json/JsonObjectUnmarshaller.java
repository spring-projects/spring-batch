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
package org.springframework.batch.infrastructure.item.json;

/**
 * Strategy interface to unmarshal a json representation into an object. It is the
 * counterpart of {@link JsonObjectMarshaller}. Implementations are expected to be given a
 * valid json object.
 *
 * @param <T> type of objects to unmarshal
 * @author Mahmoud Ben Hassine
 * @since 6.1
 */
@FunctionalInterface
public interface JsonObjectUnmarshaller<T> {

	/**
	 * Unmarshal a json representation into an object.
	 * @param json the json representation of the object
	 * @return the unmarshalled object
	 */
	T unmarshal(String json);

}
