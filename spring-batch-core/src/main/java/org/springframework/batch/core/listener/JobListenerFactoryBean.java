/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.springframework.batch.core.configuration.util.MethodInvokerUtils.getMethodInvokerByAnnotation;
import static org.springframework.batch.core.configuration.util.MethodInvokerUtils.getMethodInvokerForInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.util.MethodInvoker;
import org.springframework.batch.core.configuration.util.MethodInvokerUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} implementation that builds a {@link JobExecutionListener}
 * based on the various lifecycle methods or annotations that are provided.
 * There are three possible ways of having a method called as part of a
 * {@link JobExecutionListener} lifecyle:
 * 
 * <ul>
 * <li>Interface implementation: By implementing JobExecutionListener, methods
 * on said interface will be called.
 * <li>Annotations: Annotating a method will result in registration.
 * <li>String name of the method to be called, which is tied to
 * {@link JobListenerMetaData} in the metaDatMap.
 * </ul>
 * 
 * It should be noted that methods obtained by name or annotation that don't
 * match the listener method signatures to which they belong, will cause errors.
 * However, it is acceptable to have no parameters at all. If the same method is
 * marked in more than one way. (i.e. the method name is given and it's
 * annotated) the method will only be called once. However, if the same class
 * has multiple methods tied to a particular listener, each method will be
 * called.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see JobListenerMetaData
 */
public class JobListenerFactoryBean implements FactoryBean, InitializingBean {

	private Object delegate;

	private Map<String, String> metaDataMap;

	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}

	public void setMetaDataMap(Map<String, String> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	public Object getObject() {
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		if (metaDataMap == null) {
			metaDataMap = new HashMap<String, String>();
		}
		// Because all annotations and interfaces should be checked for, make
		// sure that each meta data
		// entry is represented.
		for (JobListenerMetaData metaData : JobListenerMetaData.values()) {
			if (!metaDataMap.containsKey(metaData.getPropertyName())) {
				// put null so that the annotation and interface is checked
				metaDataMap.put(metaData.getPropertyName(), null);
			}
		}

		// For every entry in the map, try and find a method by interface, name,
		// or annotation. If the same
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			JobListenerMetaData metaData = JobListenerMetaData.fromPropertyName(entry.getKey());
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByName(entry.getValue(), delegate, JobExecution.class));
			invokers.add(getMethodInvokerForInterface(JobExecutionListener.class, metaData.getMethodName(), delegate,
					JobExecution.class));
			invokers.add(getMethodInvokerByAnnotation(metaData.getAnnotation(), delegate));
			if (!invokers.isEmpty()) {
				invokerMap.put(metaData.getMethodName(), invokers);
			}
		}

		// create a proxy listener for only the interfaces that have methods to
		// be called
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(delegate);
		proxyFactory.setInterfaces(new Class[] { JobExecutionListener.class });
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(new MethodInvokerMethodInterceptor(invokerMap)));
		return proxyFactory.getProxy();
	}

	private MethodInvoker getMethodInvokerByName(String methodName, Object candidate, Class<?>... params) {
		if (methodName != null) {
			return MethodInvokerUtils.createMethodInvokerByName(candidate, methodName, false, params);
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public Class getObjectType() {
		return JobExecutionListener.class;
	}

	public boolean isSingleton() {
		return false;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "Delegate listener must not be null");
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link JobExecutionListener} interfaces.
	 * @param delegate a delegate object
	 * @return a JobListener instance constructed from the delegate
	 */
	public static JobExecutionListener getListener(Object delegate) {
		JobListenerFactoryBean factory = new JobListenerFactoryBean();
		factory.setDelegate(delegate);
		return (JobExecutionListener) factory.getObject();
	}

	/**
	 * Convenience method to check whether the given object is or can be made
	 * into a {@link JobExecutionListener}.
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of
	 * {@link JobExecutionListener}, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate) {
		if (delegate instanceof JobExecutionListener) {
			return true;
		}
		for (JobListenerMetaData metaData : JobListenerMetaData.values()) {
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByAnnotation(metaData.getAnnotation(), delegate));
			if (!invokers.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Extension of HashSet that ignores nulls, rather than putting them into
	 * the set.
	 */
	private static class NullIgnoringSet<E> extends HashSet<E> {

		@Override
		public boolean add(E e) {
			if (e == null) {
				return false;
			}
			else {
				return super.add(e);
			}
		};
	}
}
