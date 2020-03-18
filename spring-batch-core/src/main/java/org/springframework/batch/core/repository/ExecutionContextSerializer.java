/*
 * Copyright 2006-2012 the original author or authors.
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
package org.springframework.batch.core.repository;

import java.util.Map;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;

/**
 * A composite interface that combines both serialization and deserialization
 * of an execution context into a single implementation.  Implementations of this
 * interface are used to serialize the execution context for persistence during
 * the execution of a job.
 *
 * @author Michael Minella
 * @since 2.2
 * @see Serializer
 * @see Deserializer
 */
public interface ExecutionContextSerializer extends Serializer<Map<String, Object>>, Deserializer<Map<String, Object>> {

}
