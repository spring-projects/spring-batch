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

package org.springframework.batch.core.scope.util;

import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.DefaultScopedObject;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * Factory bean for proxies that can replace placeholders in their target. Just
 * a specialisation of {@link ScopedProxyFactoryBean}, with a different target
 * source type.
 * 
 * @author Dave Syer
 * 
 */
public class PlaceholderProxyFactoryBean extends ProxyConfig implements FactoryBean, BeanFactoryAware {

	/** The TargetSource that manages scoping */
	private final PlaceholderTargetSource scopedTargetSource = new PlaceholderTargetSource();

	/** The name of the target bean */
	private String targetBeanName;

	/** The cached singleton proxy */
	private Object proxy;

	private final ContextFactory contextFactory;

	/**
	 * Create a new FactoryBean instance.
	 */
	public PlaceholderProxyFactoryBean(ContextFactory contextFactory) {
		this.contextFactory = contextFactory;
		// setProxyTargetClass(false);
	}

	/**
	 * Set the name of the bean that is to be scoped.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
		this.scopedTargetSource.setTargetBeanName(targetBeanName);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
		}
		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;

		this.scopedTargetSource.setBeanFactory(beanFactory);
		this.scopedTargetSource.setContextFactory(contextFactory);

		ProxyFactory pf = new ProxyFactory();
		pf.copyFrom(this);
		pf.setTargetSource(this.scopedTargetSource);

		Class<?> beanType = beanFactory.getType(this.targetBeanName);
		if (beanType == null) {
			throw new IllegalStateException("Cannot create scoped proxy for bean '" + this.targetBeanName
					+ "': Target type could not be determined at the time of proxy creation.");
		}
		if (!isProxyTargetClass() || beanType.isInterface() || Modifier.isPrivate(beanType.getModifiers())) {
			pf.setInterfaces(ClassUtils.getAllInterfacesForClass(beanType, cbf.getBeanClassLoader()));
		}

		// Add an introduction that implements only the methods on ScopedObject.
		ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName());
		pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject));

		// Add the AopInfrastructureBean marker to indicate that the scoped
		// proxy itself is not subject to auto-proxying! Only its target bean
		// is.
		pf.addInterface(AopInfrastructureBean.class);
		
		this.scopedTargetSource.afterPropertiesSet();

		this.proxy = pf.getProxy(cbf.getBeanClassLoader());
	}

	public Object getObject() {
		if (this.proxy == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.proxy;
	}

	public Class<?> getObjectType() {
		if (this.proxy != null) {
			return this.proxy.getClass();
		}
		if (this.scopedTargetSource != null) {
			return this.scopedTargetSource.getTargetClass();
		}
		return null;
	}

	public boolean isSingleton() {
		return true;
	}

	/**
	 * Convenience method to create a {@link BeanDefinition} for a target
	 * wrapped in a placeholder tarrget source, able to defer binding of
	 * placeholders until the bean is used.
	 * 
	 * @param definition a target bean definition
	 * @param registry a {@link BeanDefinitionRegistry}
	 * @param proxyTargetClass true if we need to use CGlib to create the
	 * proxies
	 * @return a {@link BeanDefinitionHolder} for a
	 * {@link PlaceholderProxyFactoryBean}
	 */
	public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
			BeanDefinitionRegistry registry, boolean proxyTargetClass) {

		String originalBeanName = definition.getBeanName();
		BeanDefinition targetDefinition = definition.getBeanDefinition();

		// Create a proxy definition for the original bean name,
		// "hiding" the target bean in an internal target definition.
		RootBeanDefinition proxyDefinition = new RootBeanDefinition(PlaceholderProxyFactoryBean.class);
		proxyDefinition.getConstructorArgumentValues().addGenericArgumentValue(new StepContextFactory());
		proxyDefinition.setOriginatingBeanDefinition(definition.getBeanDefinition());
		proxyDefinition.setSource(definition.getSource());
		proxyDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		String targetBeanName = "lazyBindingProxy." + originalBeanName;
		proxyDefinition.getPropertyValues().addPropertyValue("targetBeanName", targetBeanName);

		if (proxyTargetClass) {
			targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ProxyFactoryBean's "proxyTargetClass" default is FALSE, so we
			// need to set it explicitly here.
			proxyDefinition.getPropertyValues().addPropertyValue("proxyTargetClass", Boolean.TRUE);
		}
		else {
			proxyDefinition.getPropertyValues().addPropertyValue("proxyTargetClass", Boolean.FALSE);
		}

		proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
		// The target bean should be ignored in favor of the proxy.
		targetDefinition.setAutowireCandidate(false);

		// Register the target bean as separate bean in the factory.
		registry.registerBeanDefinition(targetBeanName, targetDefinition);

		// Return the scoped proxy definition as primary bean definition
		// (potentially an inner bean).
		return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
	}

}
