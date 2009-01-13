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

package org.springframework.batch.repeat.support;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Interface for result holder. 
 * 
 * @author Dave Syer
 */
interface ResultHolder {
	/**
	 * Get the result for client from this holder. Does not block if none is
	 * available yet.
	 * 
	 * @return the result, or null if there is none.
	 * @throws IllegalStateException
	 */
	RepeatStatus getResult();

	/**
	 * Get the error for client from this holder if any. Does not block if
	 * none is available yet.
	 * 
	 * @return the error, or null if there is none.
	 * @throws IllegalStateException
	 */
	Throwable getError();

	/**
	 * Get the context in which the result evaluation is executing.
	 * 
	 * @return the context of the result evaluation.
	 */
	RepeatContext getContext();
}