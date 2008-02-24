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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.ClassUtils;

/**
 * Simple {@link StreamManager} that tries to resolve conflicts between key
 * names by using the class name of a stream to prefix property keys.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStreamManager implements StreamManager {

	private Map registry = new HashMap();

	private PlatformTransactionManager transactionManager;

	private boolean useClassNameAsPrefix = true;

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
	 * Public setter for the flag. If this is true then the class name of the
	 * streams will be used as a prefix in the {@link ExecutionContext} in
	 * {@link #getExecutionContext(Object)}. The default value is true, which
	 * gives the best chance of unique key names in the context.
	 * 
	 * @param useClassNameAsPrefix the flag to set (default true).
	 */
	public void setUseClassNameAsPrefix(boolean useClassNameAsPrefix) {
		this.useClassNameAsPrefix = useClassNameAsPrefix;
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
	public ExecutionContext getExecutionContext(Object key) {
		final ExecutionContext result = new ExecutionContext();
		iterate(key, new Callback() {
			public void execute(ItemStream stream) {
				ExecutionContext context = stream.getExecutionContext();
				String prefix = ClassUtils.getQualifiedName(stream.getClass()) + ".";
				if (!useClassNameAsPrefix) {
					prefix = "";
				}
				for (Iterator iterator = context.entrySet().iterator(); iterator.hasNext();) {
					Entry entry = (Entry) iterator.next();
					String contextKey = prefix + entry.getKey();
					result.put(contextKey, entry.getValue());
				}
			}
		});
		return result;
	}

	/**
	 * Register a {@link ItemStream} as one of the interesting providers under
	 * the provided key.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#register(java.lang.Object,
	 * org.springframework.batch.item.ItemStream, ExecutionContext)
	 */
	public void register(Object key, ItemStream stream) {
		synchronized (registry) {
			Set set = (Set) registry.get(key);
			if (set == null) {
				set = new LinkedHashSet();
				registry.put(key, set);
			}
			set.add(stream);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.stream.StreamManager#restoreFrom(java.lang.Object, org.springframework.batch.item.ExecutionAttributes)
	 */
	public void restoreFrom(Object key, final ExecutionContext executionContext) {
		if (executionContext != null) {
			iterate(key, new Callback() {
				public void execute(ItemStream stream) {
					stream.restoreFrom(extract(stream, executionContext));
				}
			});
		}	
	}

	/**
	 * @param stream
	 * @param executionContext
	 * @return
	 */
	private ExecutionContext extract(ItemStream stream, ExecutionContext executionContext) {
		ExecutionContext result = new ExecutionContext();
		String prefix = ClassUtils.getQualifiedName(stream.getClass()) + ".";
		if (!useClassNameAsPrefix) {
			prefix = "";
		}
		for (Iterator iterator = executionContext.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			String contextKey = (String) entry.getKey();
			if (contextKey.startsWith(prefix)) {
				result.putString(contextKey.substring(prefix.length()), entry.getValue().toString());
			}
		}
		return result;
	}

	/**
	 * Broadcast the call to close from this {@link StreamManager}.
	 * @throws StreamException
	 */
	public void close(Object key) throws StreamException {
		iterate(key, new Callback() {
			public void execute(ItemStream stream) {
				stream.close();
			}
		});
	}

	/**
	 * Broadcast the call to open from this {@link StreamManager}.
	 * @throws StreamException
	 */
	public void open(Object key) throws StreamException {
		iterate(key, new Callback() {
			public void execute(ItemStream stream) {
				stream.open();
			}
		});
	}

	/**
	 * Delegate to the {@link PlatformTransactionManager} to create a new
	 * transaction.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#getTransaction()
	 */
	public TransactionStatus getTransaction(final Object key) {
		TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
		return transaction;
	}

	/**
	 * @param key
	 */
	private void iterate(Object key, Callback callback) {
		Set set = new LinkedHashSet();
		synchronized (registry) {
			Collection collection = (Collection) registry.get(key);
			if (collection != null) {
				set.addAll(collection);
			}
		}
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			ItemStream stream = (ItemStream) iterator.next();
			callback.execute(stream);
		}
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

	private interface Callback {
		void execute(ItemStream stream);
	}

}
