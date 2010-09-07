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

package org.springframework.batch.item;

/**
 * Strategy interface for providing the data. <br/>
 * 
 * Implementations are expected to be stateful and will be called multiple times
 * for each batch, with each call to {@link #read()} returning a different value
 * and finally returning <code>null</code> when all input data is exhausted.<br/>
 * 
 * Implementations need *not* be thread safe and clients of a {@link ItemReader}
 * need to be aware that this is the case.<br/>
 * 
 * A richer interface (e.g. with a look ahead or peek) is not feasible because
 * we need to support transactions in an asynchronous batch.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 * @author Lucas Ward
 * @since 1.0
 */
public interface ItemReader<T> {

	/**
	 * Reads a piece of input data and advance to the next one. Implementations
	 * <strong>must</strong> return <code>null</code> at the end of the input
	 * data set. In a transactional setting, caller might get the same item
	 * twice from successive calls (or otherwise), if the first call was in a
	 * transaction that rolled back.
	 * 
	 * @throws ParseException if there is a problem parsing the current record
	 * (but the next one may still be valid)
	 * @throws NonTransientResourceException if there is a fatal exception in
	 * the underlying resource. After throwing this exception implementations
	 * should endeavour to return null from subsequent calls to read.
	 * @throws UnexpectedInputException if there is an uncategorised problem
	 * with the input data. Assume potentially transient, so subsequent calls to
	 * read might succeed.
	 * @throws Exception if an there is a non-specific error.
	 */
	T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException;

}
