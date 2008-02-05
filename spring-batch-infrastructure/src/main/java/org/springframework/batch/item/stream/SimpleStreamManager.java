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

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
	 * streams will be used as a prefix in the {@link StreamContext} in
	 * {@link #getStreamContext(Object)}. The default value is true, which
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
	 * Simple aggregate {@link StreamContext} provider for the contributions
	 * registered under the given key.
	 * 
	 * @see org.springframework.batch.item.stream.StreamManager#getStreamContext(java.lang.Object)
	 */
	public ExecutionAttributes getStreamContext(Object key) {
		final ExecutionAttributes result = new ExecutionAttributes();
		iterate(key, new Callback() {
			public void execute(ItemStream stream) {
				ExecutionAttributes context = stream.getStreamContext();
				String prefix = ClassUtils.getQualifiedName(stream.getClass()) + ".";
				if (!useClassNameAsPrefix) {
					prefix = "";
				}
				for (Iterator iterator = context.entrySet().iterator(); iterator.hasNext();) {
					Entry entry = (Entry) iterator.next();
					String contextKey = prefix + entry.getKey();
					result.putString(contextKey, entry.getValue().toString());
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
	 * org.springframework.batch.item.ItemStream, StreamContext)
	 */
	public void register(Object key, ItemStream stream, ExecutionAttributes streamContext) {
		synchronized (registry) {
			Set set = (Set) registry.get(key);
			if (set == null) {
				set = new LinkedHashSet();
				registry.put(key, set);
			}
			set.add(stream);
		}
		if (streamContext != null) {
			stream.restoreFrom(extract(stream, streamContext));
		}
	}

	/**
	 * @param stream
	 * @param streamContext
	 * @return
	 */
	private ExecutionAttributes extract(ItemStream stream, ExecutionAttributes context) {
		ExecutionAttributes result = new ExecutionAttributes();
		String prefix = ClassUtils.getQualifiedName(stream.getClass()) + ".";
		if (!useClassNameAsPrefix) {
			prefix = "";
		}
		for (Iterator iterator = context.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			String contextKey = (String) entry.getKey();
			if (contextKey.startsWith(prefix)) {
				result.putString(contextKey.substring(prefix.length()), entry.getValue().toString());
			}
		}
		return result;
	}

	/**
	 * Broadcast the call to close from this {@link StreamContext}.
	 * @throws Exception
	 * 
	 * @see StreamManager#restoreFrom(Object, StreamContext)
	 */
	public void close(Object key) throws StreamException {
		iterate(key, new Callback() {
			public void execute(ItemStream stream) {
				stream.close();
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
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_COMMITTED) {
					iterate(key, new Callback() {
						public void execute(ItemStream stream) {
							if (stream.isMarkSupported()) {
								stream.mark(stream.getStreamContext());
							}
						}
					});
				}
				else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
					iterate(key, new Callback() {
						public void execute(ItemStream stream) {
							if (stream.isMarkSupported()) {
								stream.reset(stream.getStreamContext());
							}
						}
					});
				}
			}
		});
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
