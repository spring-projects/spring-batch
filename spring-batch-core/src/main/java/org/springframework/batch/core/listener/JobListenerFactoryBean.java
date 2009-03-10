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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.support.MethodInvoker;
import org.springframework.core.Ordered;
import org.springframework.batch.core.listener.MethodInvokerMethodInterceptor;

/**
 * This {@link AbstractListenerFactoryBean} implementation is used to create a
 * {@link JobExecutionListener}.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerFactoryBean
 * @see JobListenerMetaData
 */
public class JobListenerFactoryBean extends AbstractListenerFactoryBean {

	public Object doGetObject(Object delegate, Map<String, String> metaDataMap) {

		// For every entry in the map, try and find a method by interface, name,
		// or annotation. If the same
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			JobListenerMetaData metaData = JobListenerMetaData.fromPropertyName(entry.getKey());
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByName(entry.getValue(), delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerForInterface(JobExecutionListener.class, metaData.getMethodName(), delegate,
					JobExecution.class));
			invokers.add(getMethodInvokerByAnnotation(metaData));
			if (!invokers.isEmpty()) {
				invokerMap.put(metaData.getMethodName(), invokers);
			}
		}

		// create a proxy listener for only the interfaces that have methods to
		// be called
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(delegate);
		
		boolean ordered = false;
		if (delegate instanceof Ordered) {
			ordered = true;
			proxyFactory.addInterface(Ordered.class);
		}

		proxyFactory.addInterface(JobExecutionListener.class);
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(new MethodInvokerMethodInterceptor(invokerMap, ordered)));
		return proxyFactory.getProxy();
	}

	protected AbstractListenerMetaData[] getMetaDataValues() {
		return JobListenerMetaData.values();
	}

	@SuppressWarnings("unchecked")
	public Class getObjectType() {
		return JobExecutionListener.class;
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link JobExecutionListener} interfaces.
	 * 
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
	 * 
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of
	 *         {@link JobExecutionListener}, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate) {
		return AbstractListenerFactoryBean.isListener(delegate, JobExecutionListener.class, JobListenerMetaData
				.values());
	}
}
