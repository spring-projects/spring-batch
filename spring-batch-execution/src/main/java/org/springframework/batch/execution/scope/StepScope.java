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
package org.springframework.batch.execution.scope;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.Ordered;

/**
 * Scope for step context. Objects in this scope with &lt;aop:scoped-proxy/&gt;
 * use the Spring container as an object factory, so there is only one instance
 * of such a bean per executing step.
 * 
 * @author Dave Syer
 * 
 */
public class StepScope implements Scope, BeanFactoryPostProcessor, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Object mutex = new Object();

	/**
	 * @param order the order value to set priority of callback execution for
	 * the {@link BeanFactoryPostProcessor} part of this scope bean.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public int getOrder() {
		return order;
	}

	private String name = "step";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.config.Scope#get(java.lang.String,
	 * org.springframework.beans.factory.ObjectFactory)
	 */
	public Object get(String name, ObjectFactory objectFactory) {
		StepContext context = getContext();
		Object scopedObject = context.getAttribute(name);
		if (scopedObject == null) {
			synchronized (mutex) {
				scopedObject = context.getAttribute(name);
				if (scopedObject == null) {
					scopedObject = objectFactory.getObject();
					if (scopedObject instanceof StepContextAware) {
						((StepContextAware) scopedObject).setStepContext(context);
					}
					context.setAttribute(name, scopedObject);
				}
			}
		}
		return scopedObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.config.Scope#getConversationId()
	 */
	public String getConversationId() {
		StepContext context = getContext();
		return context.getIdentifier();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.config.Scope#registerDestructionCallback(java.lang.String,
	 * java.lang.Runnable)
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		StepContext context = getContext();
		context.registerDestructionCallback(name, callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.config.Scope#remove(java.lang.String)
	 */
	public Object remove(String name) {
		StepContext context = getContext();
		return context.removeAttribute(name);
	}

	/**
	 * Get an attribute accessor in the form of a {@link SimpleStepContext} that
	 * can be used to store scoped bean instances.
	 * 
	 * @return the current step context which we can use as a scope storage
	 * medium
	 */
	private StepContext getContext() {
		StepContext context = StepSynchronizationManager.getContext();
		if (context == null) {
			throw new IllegalStateException("No context holder available for step scope");
		}
		return context;
	}

	/**
	 * Register this scope with the enclosing BeanFactory.
	 * 
	 * @param beanFactory the BeanFactory to register with
	 * @throws BeansException if there is a problem.
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.registerScope(name, this);
	}

	/**
	 * Public setter for the name property. This can then be used as a bean
	 * definition attribute, e.g. scope="step". Defaults to "step".
	 * 
	 * @param name the name to set for this scope.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
