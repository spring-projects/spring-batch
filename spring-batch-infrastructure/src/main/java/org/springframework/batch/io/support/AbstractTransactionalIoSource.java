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
package org.springframework.batch.io.support;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.stream.ItemStreamAdapter;

/**
 * <p>
 * Abstract class that abstracts away transaction handling from input and
 * output. Any {@link ItemReader} or {@link ItemWriter} that wants to be
 * notified of transaction events to maintain the contract that all calls to
 * read or write can extend this base class to ensure that correct ordering is
 * maintained regardless of rollbacks.
 * </p>
 * 
 * @author Lucas Ward
 * @since 1.0
 */
public abstract class AbstractTransactionalIoSource extends ItemStreamAdapter {

	/*
	 * Called when a transaction has been committed.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	public abstract void mark(ExecutionAttributes executionAttributes);

	/*
	 * Called when a transaction has been rolled back.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	public abstract void reset(ExecutionAttributes executionAttributes);

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		return true;
	}
	
}
