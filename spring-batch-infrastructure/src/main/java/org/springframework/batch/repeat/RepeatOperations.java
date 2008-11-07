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
 * The main interface providing access to batch operations. The batch client is
 * the {@link RepeatCallback}, where a single item or record is processed. The
 * batch behaviour, boundary conditions, transactions etc, are dealt with by the
 * {@link RepeatOperations} in such as way that the client does not need to know
 * about them. The client may have access to framework abstractions, like
 * template data sources, but these should work the same whether they are in a
 * batch or not.
 * 
 * @author Dave Syer
 * 
 */
public interface RepeatOperations {

	/**
	 * Execute the callback repeatedly, until a decision can be made to
	 * complete. The decision about how many times to execute or when to
	 * complete, and what to do in the case of an error is delegated to a
	 * {@link CompletionPolicy}.
	 * 
	 * @param callback the batch callback.
	 * @return the aggregate of the result of all the callback operations. An
	 * indication of whether the {@link RepeatOperations} can continue
	 * processing if this method is called again.
	 */
	RepeatStatus iterate(RepeatCallback callback) throws RepeatException;

}
