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

package org.springframework.batch.support.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Factory for transaction aware objects (like lists, sets, maps). If a
 * transaction is active when a method is called on an instance created by the
 * factory, it makes a copy of the target object and carries out all operations
 * on the copy. Only when the transaction commits is the target re-initialised
 * with the copy.<br/>
 * 
 * Works well with collections and maps for testing transactional behaviour
 * without needing a database. The base implementation handles lists, sets and
 * maps. Subclasses can implement {@link #begin(Object)} and
 * {@link #commit(Object, Object)} to provide support for other resources.<br/>
 * 
 * Not intended for multi-threaded use.
 * 
 * @author Dave Syer
 * 
 */
public class TransactionAwareProxyFactory<T> {

	private T target;

	private TransactionAwareProxyFactory(T target) {
		super();
		this.target = begin(target);
	}

	/**
	 * Make a copy of the target that can be used inside a transaction to
	 * isolate changes from the original. Also called from the factory
	 * constructor to isolate the target from the original value passed in.
	 * 
	 * @param target the target object (List, Set or Map)
	 * @return an independent copy
	 */
	@SuppressWarnings("unchecked")
	protected final T begin(T target) {
		if (target instanceof List) {
			return (T) new ArrayList((List) target);
		}
		else if (target instanceof Set) {
			return (T) new HashSet((Set) target);
		}
		else if (target instanceof Map) {
			return (T) new HashMap((Map) target);
		}
		else {
			throw new UnsupportedOperationException("Cannot copy target for this type: " + target.getClass());
		}
	}

	/**
	 * Take the working copy state and commit it back to the original target.
	 * The target then reflects all the changes applied to the copy during a
	 * transaction.
	 * 
	 * @param copy the working copy.
	 * @param target the original target of the factory.
	 */
	@SuppressWarnings("unchecked")
	protected void commit(T copy, T target) {
		if (target instanceof Collection) {
			((Collection) target).clear();
			((Collection) target).addAll((Collection) copy);
		}
		else {
			((Map) target).clear();
			((Map) target).putAll((Map) copy);
		}
	}

	private T createInstance() {
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice(new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) throws Throwable {

				if (!TransactionSynchronizationManager.isActualTransactionActive()) {
					return invocation.proceed();
				}

				T cache;

				if (!TransactionSynchronizationManager.hasResource(this)) {
					cache = begin(target);
					TransactionSynchronizationManager.bindResource(this, cache);
					TransactionSynchronizationManager.registerSynchronization(new TargetSynchronization(this, cache));
				}
				else {
					@SuppressWarnings("unchecked")
					T retrievedCache = (T) TransactionSynchronizationManager.getResource(this);
					cache = retrievedCache;
				}

				return invocation.getMethod().invoke(cache, invocation.getArguments());

			}
		});
		@SuppressWarnings("unchecked")
		T instance = (T) factory.getProxy();
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> createTransactionalMap() {
		return (Map<K,V>) new TransactionAwareProxyFactory(new HashMap<K,V>()).createInstance();
	}

	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> createTransactionalMap(Map<K,V> map) {
		return (Map<K,V>) new TransactionAwareProxyFactory(map).createInstance();
	}

	@SuppressWarnings("unchecked")
	public static <T> Set<T> createTransactionalSet() {
		return (Set<T>) new TransactionAwareProxyFactory(new HashSet<T>()).createInstance();
	}

	@SuppressWarnings("unchecked")
	public static <T> Set<T> createTransactionalSet(Set<T> set) {
		return (Set<T>) new TransactionAwareProxyFactory(set).createInstance();
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> createTransactionalList() {
		return (List<T>) new TransactionAwareProxyFactory(new ArrayList<T>()).createInstance();
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> createTransactionalList(List<T> list) {
		return (List<T>) new TransactionAwareProxyFactory(list).createInstance();
	}

	private class TargetSynchronization extends TransactionSynchronizationAdapter {

		T cache;

		Object key;

		public TargetSynchronization(Object key, T cache) {
			super();
			this.cache = cache;
			this.key = key;
		}

		public void afterCompletion(int status) {
			super.afterCompletion(status);
			if (status == TransactionSynchronization.STATUS_COMMITTED) {
				synchronized (target) {
					commit(cache, target);
				}
			}
			TransactionSynchronizationManager.unbindResource(key);
		}
	}

}
