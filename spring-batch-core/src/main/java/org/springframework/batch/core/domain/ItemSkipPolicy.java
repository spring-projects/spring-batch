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
package org.springframework.batch.core.domain;


/**
 * Policy for determining whether or not an item should be skipped.
 * 
 * @author Lucas Ward
 */
public interface ItemSkipPolicy {

	/**
	 * Returns true or false, indicating whether or not reading should
	 * continue for the current step execution with the given throwable.
	 * 
	 * @param exception throwable encountered while reading
	 * @param skipCount currently running count of skips
	 * @return true if reading should continue, false otherwise.
	 * @throws IllegalArgumentException if the exception is null
	 */
	boolean shouldSkip(Exception exception, int skipCount);
}
