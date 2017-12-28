/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.attribute;

/**
 * A Setter sets a value of an object
 * @author Thomas Nill
 * @since 4.0.1
 * 
 * @param <T> a tape of the objects with will be set 
 * @param <V> the type of the parameters 
 * @see Setter
 */
@FunctionalInterface
public interface Setter<T, V> {

	/**
	 * set the object obj to the value
	 * 
	 * @param obj the object that will be set
	 * @param value the value to with the object will be set
	 */
	void setValue(T obj, V value);
}
