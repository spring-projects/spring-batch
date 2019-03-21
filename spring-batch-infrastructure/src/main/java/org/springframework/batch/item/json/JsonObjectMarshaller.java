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

/**
 * Strategy interface to marshal an object into a json representation.
 * Implementations are required to return a valid json object.
 *
 * @param <T> type of objects to marshal
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public interface JsonObjectMarshaller<T> {

	/**
	 * Marshal an object into a json representation.
	 * @param object to marshal
	 * @return json representation fo the object
	 */
	String marshal(T object);

}
