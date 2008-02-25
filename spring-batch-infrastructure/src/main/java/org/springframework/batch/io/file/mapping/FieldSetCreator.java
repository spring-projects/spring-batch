/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.io.file.mapping;

import org.springframework.batch.io.file.transform.LineTokenizer;

/**
 * Strategy interface for mapping between arbitrary objects and {@link FieldSet}.
 * Similar to a {@link LineTokenizer}, but the input is generally a domain
 * object, not a String.
 * 
 * @author Dave Syer
 * 
 */
public interface FieldSetCreator {

	/**
	 * @param data an Object to convert.
	 * @return a {@link FieldSet} created from the input.
	 */
	FieldSet mapItem(Object data);

}
