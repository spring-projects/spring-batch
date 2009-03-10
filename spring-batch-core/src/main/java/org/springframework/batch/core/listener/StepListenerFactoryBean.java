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
import org.springframework.core.Ordered;

/**
 * This {@link AbstractListenerFactoryBean} implementation is used to create a
 * {@link StepListener}.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerFactoryBean
 * @see StepListenerMetaData
 */
public class StepListenerFactoryBean extends AbstractListenerFactoryBean {

	public Object doGetObject(Object delegate, Map<String, String> metaDataMap) {

		Set<Class<?>> listenerInterfaces = new HashSet<Class<?>>();

		// For every entry in the map, try and find a method by interface, name,
		// or annotation. If the same
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			final StepListenerMetaData metaData = StepListenerMetaData.fromPropertyName(entry.getKey());
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByName(entry.getValue(), delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerForInterface(metaData.getListenerInterface(), metaData.getMethodName(),
					delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerByAnnotation(metaData));
			if (!invokers.isEmpty()) {
				invokerMap.put(metaData.getMethodName(), invokers);
				listenerInterfaces.add(metaData.getListenerInterface());
			}
		}

		if (listenerInterfaces.isEmpty()) {
			listenerInterfaces.add(StepListener.class);
		}

		boolean ordered = false;
		if (delegate instanceof Ordered) {
			ordered = true;
			listenerInterfaces.add(Ordered.class);
		}

		// create a proxy listener for only the interfaces that have methods to
		// be called
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(delegate);
		proxyFactory.setInterfaces(listenerInterfaces.toArray(new Class[0]));
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(new MethodInvokerMethodInterceptor(invokerMap, ordered)));
		return proxyFactory.getProxy();
	}

	protected AbstractListenerMetaData[] getMetaDataValues() {
		return StepListenerMetaData.values();
	}

	@SuppressWarnings("unchecked")
	public Class getObjectType() {
		return StepListener.class;
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link StepListener} interfaces.
	 * 
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
	 * 
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of any of the
	 * {@link StepListener} interfaces, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate) {
		return AbstractListenerFactoryBean.isListener(delegate, StepListener.class, StepListenerMetaData.values());
	}
}
