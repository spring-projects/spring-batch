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
package org.springframework.batch.item.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Simple {@link StreamManager} that tries to resolve conflicts between key
 * names by using the class name of a stream to prefix property keys.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStreamManager implements StreamManager {

	private List streams = new ArrayList();

	private PlatformTransactionManager transactionManager;

	/**
	 * @param transactionManager a {@link PlatformTransactionManager}
	 */
	public SimpleStreamManager(PlatformTransactionManager transactionManager) {
		this();
		this.transactionManager = transactionManager;
	}

	/**
	 * 
	 */
	public SimpleStreamManager() {
		super();
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the {@link PlatformTransactionManager} to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Simple aggregate {@link ExecutionContext} provider for the contributions
	 * registered under the given key.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#getExecutionContext(java.lang.Object)
	 */
	public void update(ExecutionContext executionContext) {
		synchronized (streams) {
			for (Iterator it = streams.iterator(); it.hasNext();) {
				ItemStream itemStream = (ItemStream) it.next();
				itemStream.update(executionContext);
			}
		}
	}

	/**
	 * Register a {@link ItemStream} as one of the interesting providers under
	 * the provided key.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#register(java.lang.Object,
	 * org.springframework.batch.item.ItemStream, ExecutionContext)
	 */
	public void register(ItemStream stream) {
		synchronized (streams) {
			if (!streams.contains(stream)) {
				streams.add(stream);
			}
		}
	}

	/**
	 * Broadcast the call to close from this {@link StreamManager}.
	 * @throws StreamException
	 */
	public void close(ExecutionContext executionContext) throws StreamException {
		synchronized (streams) {
			for (Iterator it = streams.iterator(); it.hasNext();) {
				ItemStream itemStream = (ItemStream) it.next();
				itemStream.close(executionContext);
			}
			streams.clear();
		}
	}

	/**
	 * Broadcast the call to open from this {@link StreamManager}.
	 * @throws StreamException
	 */
	public void open(ExecutionContext executionContext) throws StreamException {
		synchronized (streams) {
			for (Iterator it = streams.iterator(); it.hasNext();) {
				ItemStream itemStream = (ItemStream) it.next();
				itemStream.open(executionContext);
			}
		}
	}

	/**
	 * Delegate to the {@link PlatformTransactionManager} to create a new
	 * transaction.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#getTransaction()
	 */
	public TransactionStatus getTransaction() {
		TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
		return transaction;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.stream.StreamManager#commit(java.lang.Object)
	 */
	public void commit(TransactionStatus status) {
		transactionManager.commit(status);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.stream.StreamManager#rollback(java.lang.Object)
	 */
	public void rollback(TransactionStatus status) {
		transactionManager.rollback(status);
	}
}
