/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.repository.dao;

import java.util.Map;

/**
 * Interface defining serialization support for execution context Map in the form of a String.
 *  
 * @author Thomas Risberg
 * @since 2.0
 */
public interface ExecutionContextStringSerializer {

	/**
	 * Serialize the context to a string representation
	 *
	 * @param context the object that should be serialized
	 * @return the serialization string
	 */
	String serialize(Map<String, Object> context);

	/**
	 * De-serialize the context from a string representation
	 *
	 * @param context the serialization string that should be de-serialized
	 * @return the de-serialized context map
	 */
	Map<String, Object> deserialize(String context);
	
}
