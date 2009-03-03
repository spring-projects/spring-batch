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

import static org.springframework.batch.support.MethodInvokerUtils.getMethodInvokerByAnnotation;
import static org.springframework.batch.support.MethodInvokerUtils.getMethodInvokerForInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.support.MethodInvoker;
import org.springframework.batch.support.MethodInvokerUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} implementation that builds a {@link StepListener} based
 * on the various lifecycle methods or annotations that are provided. There are
 * three possible ways of having a method called as part of a
 * {@link StepListener} lifecyle:
 * 
 * <ul>
 * <li>Interface implementation: By implementing any of the subclasses of
 * StepListener, methods on said interface will be called
 * <li>Annotations: Annotating a method will result in registration.
 * <li>String name of the method to be called, which is tied to
 * {@link StepListenerMetaData} in the metaDatMap.
 * </ul>
 * 
 * It should be noted that methods obtained by name or annotation that don't
 * match the StepListener method signatures to which they belong, will cause
 * errors. However, it is acceptable to have no parameters at all. If the same
 * method is marked in more than one way. (i.e. the method name is given and
 * it's annotated) the method will only be called once. However, if the same
 * class has multiple methods tied to a particular listener, each method will be
 * called.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see StepListenerMetaData
 */
public class StepListenerFactoryBean implements FactoryBean, InitializingBean {

	private Object delegate;

	private Map<String, String> metaDataMap;

	public Object getObject() {

		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		if (metaDataMap == null) {
			metaDataMap = new HashMap<String, String>();
		}
		// Because all annotations and interfaces should be checked for, make
		// sure that each meta data
		// entry is represented.
		for (StepListenerMetaData metaData : StepListenerMetaData.values()) {
			if (!metaDataMap.containsKey(metaData.getPropertyName())) {
				// put null so that the annotation and interface is checked
				metaDataMap.put(metaData.getPropertyName(), null);
			}
		}

		Set<Class<? extends StepListener>> listenerInterfaces = new HashSet<Class<? extends StepListener>>();

		// For every entry in the map, try and find a method by interface, name,
		// or annotation. If the same
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			StepListenerMetaData metaData = StepListenerMetaData.fromPropertyName(entry.getKey());
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByName(entry.getValue(), delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerForInterface(metaData.getListenerInterface(), metaData.getMethodName(),
					delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerByAnnotation(metaData.getAnnotation(), delegate));
			if (!invokers.isEmpty()) {
				invokerMap.put(metaData.getMethodName(), invokers);
				listenerInterfaces.add(metaData.getListenerInterface());
			}
		}

		if (listenerInterfaces.isEmpty()) {
			listenerInterfaces.add(StepListener.class);
		}

		// create a proxy listener for only the interfaces that have methods to
		// be called
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setInterfaces(listenerInterfaces.toArray(new Class[0]));
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
		return StepListener.class;
	}

	public boolean isSingleton() {
		return false;
	}

	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}

	public void setMetaDataMap(Map<String, String> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link StepListener} interfaces.
	 * @param delegate a delegate object
	 * @return a StepListener instance constructed from the delegate
	 */
	public static StepListener getListener(Object delegate) {
		StepListenerFactoryBean factory = new StepListenerFactoryBean();
		factory.setDelegate(delegate);
		return (StepListener) factory.getObject();
	}

	/**
	 * Convenience method to check whether the given object is or can be made
	 * into a {@link StepListener}.
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of any of the
	 * {@link StepListener} interfaces, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate) {
		if (delegate instanceof StepListener) {
			return true;
		}
		for (StepListenerMetaData metaData : StepListenerMetaData.values()) {
			if (getMethodInvokerByAnnotation(metaData.getAnnotation(), delegate) != null) {
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

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "Delegate must not be null");
	}
}
