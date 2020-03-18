/*
 * Copyright 2006-2018 the original author or authors.
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
package org.springframework.batch.item.file.transform;

/**
 * This class will convert an object to an array of its parts.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
public interface FieldExtractor<T> {

	/**
	 * @param item the object that contains the information to be extracted.
	 * @return an array containing item's parts
	 */
	Object[] extract(T item);

}
