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
 * {@link #commit(Object, Object)} to provide support for other resources.
 * 
 * @author Dave Syer
 * 
 */
@SuppressWarnings("unchecked")
public class TransactionAwareProxyFactory {

	private Object target;

	public TransactionAwareProxyFactory(Object target) {
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
	protected final Object begin(Object target) {
		if (target instanceof List) {
			return new ArrayList((List) target);
		}
		else if (target instanceof Set) {
			return new HashSet((Set) target);
		}
		else if (target instanceof Map) {
			return new HashMap((Map) target);
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
	protected void commit(Object copy, Object target) {
		if (target instanceof Collection) {
			((Collection) target).clear();
			((Collection) target).addAll((Collection) copy);
		}
		else {
			((Map) target).clear();
			((Map) target).putAll((Map) copy);
		}
	}

	public Object createInstance() {
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {

				if (!TransactionSynchronizationManager.isActualTransactionActive()) {
					return invocation.proceed();
				}

				Object cache;

				if (!TransactionSynchronizationManager.hasResource(this)) {
					cache = begin(target);
					TransactionSynchronizationManager.bindResource(this, cache);
					TransactionSynchronizationManager.registerSynchronization(new TargetSynchronization(this, cache));
				}
				else {
					cache = TransactionSynchronizationManager.getResource(this);
				}

				return invocation.getMethod().invoke(cache, invocation.getArguments());

			}
		});
		return factory.getProxy();
	}
	
	public static Map createTransactionalMap() {
		return (Map) new TransactionAwareProxyFactory(new HashMap()).createInstance();
	}

	public static Set createTransactionalSet() {
		return (Set) new TransactionAwareProxyFactory(new HashSet()).createInstance();
	}

	public static List createTransactionalList() {
		return (List) new TransactionAwareProxyFactory(new ArrayList()).createInstance();
	}

	private class TargetSynchronization extends TransactionSynchronizationAdapter {

		Object cache;

		Object key;

		public TargetSynchronization(Object key, Object cache) {
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
