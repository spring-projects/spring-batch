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

package org.springframework.batch.repeat;


/**
 * Callback interface for batch operations. Many simple processes will be able
 * to use off-the-shelf implementations of this interface, enabling the
 * application developer to concentrate on business logic.
 * 
 * @see RepeatOperations
 * 
 * @author Dave Syer
 * 
 */
public interface RepeatCallback {

	/**
	 * Implementations return true if they can continue processing - e.g. there
	 * is a data source that is not yet exhausted. Exceptions are not necessarily
	 * fatal - processing might continue depending on the Exception type and the
	 * implementation of the caller.
	 * 
	 * @param context the current context passed in by the caller.
	 * @return an {@link RepeatStatus} which is continuable if there is (or may
	 * be) more data to process.
	 * @throws Exception if there is a problem with the processing.
	 */
	RepeatStatus doInIteration(RepeatContext context) throws Exception;
}
