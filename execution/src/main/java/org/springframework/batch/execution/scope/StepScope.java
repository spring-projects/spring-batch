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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Scope for step context. Objects in this scope with &lt;aop:scoped-proxy/&gt;
 * use the Spring container as an object factory, so there is only one instance
 * of such a bean per executing step.
 * 
 * @author Dave Syer
 * 
 */
public class StepScope implements Scope, BeanFactoryAware, BeanPostProcessor {

	/**
	 * Context key for clients to use for conversation identifier.
	 */
	public static final String ID_KEY = "JOB_IDENTIFIER";

	/**
	 * Injection callback for BeanFactory. Ensures that the bean factory
	 * contains a BeanPostProcessor of this type (so if this bean is an inner
	 * bean it will still be applied as a post processor).
	 * 
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory listable = (DefaultListableBeanFactory) beanFactory;
			if (listable.getBeanNamesForType(getClass()).length == 0) {
				listable.addBeanPostProcessor(this);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#get(java.lang.String,
	 * org.springframework.beans.factory.ObjectFactory)
	 */
	public Object get(String name, ObjectFactory objectFactory) {
		SimpleStepContext context = getContext();
		Object scopedObject = context.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			context.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#getConversationId()
	 */
	public String getConversationId() {
		SimpleStepContext context = getContext();
		Object id = context.getAttribute(ID_KEY);
		return "" + id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#registerDestructionCallback(java.lang.String,
	 * java.lang.Runnable)
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		StepContext context = getContext();
		context.registerDestructionCallback(name, callback);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#remove(java.lang.String)
	 */
	public Object remove(String name) {
		SimpleStepContext context = getContext();
		return context.removeAttribute(name);
	}

	/**
	 * Get an attribute accessor in the form of a {@link SimpleStepContext} that
	 * can be used to store scoped bean instances.
	 * 
	 * @return the current step context which we can use as a scope storage
	 * medium
	 */
	private SimpleStepContext getContext() {
		SimpleStepContext context = StepSynchronizationManager.getContext();
		if (context == null) {
			throw new IllegalStateException("No context holder available for step scope");
		}
		return context;
	}

	/**
	 * No-op.
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Check for {@link StepContextAware} and set context.
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof StepContextAware) {
			SimpleStepContext context = getContext();
			((StepContextAware) bean).setStepScopeContext(context);
		}
		return bean;
	}

}
