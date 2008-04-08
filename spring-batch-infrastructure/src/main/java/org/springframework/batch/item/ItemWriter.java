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
 * <p>Basic interface for generic output operations. Class implementing this
 * interface will be responsible for serializing objects as necessary.
 * Generally, it is responsibility of implementing class to decide which
 * technology to use for mapping and how it should be configured.</p>
 * 
 * <p>
 * Due to the nature of batch processing, it is expected that most writers
 * will buffer output.  A flush method is provided to the interface in order
 * to ensure that any buffers can be flushed before a transaction is
 * committed.  Along the same lines, if a transaction has been rolled back,
 * then the contents of any buffers should be thrown away.
 * </p>
 * 
 * @author Dave Syer
 * @author Lucas Ward
 */
public interface ItemWriter {

	/**
	 * Process the supplied data element. Will be called multiple times during a
	 * larger batch operation. Will not be called with null data in normal
	 * operation.
	 * 
	 * @throws Exception if there are errors. If the writer is used inside a
	 * retry or a batch the framework will catch the exception and convert or
	 * rethrow it as appropriate.
	 */
	void write(Object item) throws Exception;
	
	/**
	 * Flush any buffers that are being held.  This will usually be performed
	 * prior to committing any transactions.
	 * @throws FlushFailedException TODO
	 */
	void flush() throws FlushFailedException;
	
	/**
	 * Clear any buffers that are being held.  This will usually be performed
	 * prior to rolling back any transactions.
	 * @throws ClearFailedException TODO
	 */
	void clear() throws ClearFailedException;
}
