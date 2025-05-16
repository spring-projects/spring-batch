/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.scope;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * ScopeSupport.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public abstract class BatchScopeSupport implements Scope, BeanFactoryPostProcessor, Ordered {

	private boolean autoProxy = true;

	private boolean proxyTargetClass = false;

	private String name;

	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * @param order the order value to set priority of callback execution for the
	 * {@link BeanFactoryPostProcessor} part of this scope bean.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Public setter for the name property. This can then be used as a bean definition
	 * attribute, e.g. scope="job".
	 * @param name the name to set for this scope.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Flag to indicate that proxies should use dynamic subclassing. This allows classes
	 * with no interface to be proxied. Defaults to false.
	 * @param proxyTargetClass set to true to have proxies created using dynamic
	 * subclasses
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * Flag to indicate that bean definitions need not be auto proxied. This gives control
	 * back to the declarer of the bean definition (e.g. in an &#64;Configuration class).
	 * @param autoProxy the flag value to set (default true)
	 */
	public void setAutoProxy(boolean autoProxy) {
		this.autoProxy = autoProxy;
	}

	public abstract String getTargetNamePrefix();

	/**
	 * Register this scope with the enclosing BeanFactory.
	 *
	 * @see BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)
	 * @param beanFactory the BeanFactory to register with
	 * @throws BeansException if there is a problem.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		beanFactory.registerScope(name, this);

		if (!autoProxy) {
			return;
		}

		Assert.state(beanFactory instanceof BeanDefinitionRegistry,
				"BeanFactory was not a BeanDefinitionRegistry, so JobScope cannot be used.");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			if (!beanName.startsWith(getTargetNamePrefix())) {
				BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
				// Replace this or any of its inner beans with scoped proxy if it
				// has this scope
				boolean scoped = name.equals(definition.getScope());
				Scopifier scopifier = new Scopifier(registry, name, proxyTargetClass, scoped);
				scopifier.visitBeanDefinition(definition);

				if (scoped && !definition.isAbstract()) {
					createScopedProxy(beanName, definition, registry, proxyTargetClass);
				}
			}
		}

	}

	/**
	 * Wrap a target bean definition in a proxy that defers initialization until after the
	 * {@link StepContext} is available. Amounts to adding &lt;aop-auto-proxy/&gt; to a
	 * step scoped bean.
	 * @param beanName the bean name to replace
	 * @param definition the bean definition to replace
	 * @param registry the enclosing {@link BeanDefinitionRegistry}
	 * @param proxyTargetClass true if we need to force use of dynamic subclasses
	 * @return a {@link BeanDefinitionHolder} for the new representation of the target.
	 * Caller should register it if needed to be visible at top level in bean factory.
	 */
	protected static BeanDefinitionHolder createScopedProxy(String beanName, BeanDefinition definition,
			BeanDefinitionRegistry registry, boolean proxyTargetClass) {

		BeanDefinitionHolder proxyHolder;

		proxyHolder = ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(definition, beanName), registry,
				proxyTargetClass);

		registry.registerBeanDefinition(beanName, proxyHolder.getBeanDefinition());

		return proxyHolder;

	}

	/**
	 * Helper class to scan a bean definition hierarchy and force the use of auto-proxy
	 * for step scoped beans.
	 *
	 * @author Dave Syer
	 *
	 */
	protected static class Scopifier extends BeanDefinitionVisitor {

		private final boolean proxyTargetClass;

		private final BeanDefinitionRegistry registry;

		private final String scope;

		private final boolean scoped;

		public Scopifier(BeanDefinitionRegistry registry, String scope, boolean proxyTargetClass, boolean scoped) {
			super(value -> value);
			this.registry = registry;
			this.proxyTargetClass = proxyTargetClass;
			this.scope = scope;
			this.scoped = scoped;
		}

		@Override
		protected Object resolveValue(Object value) {

			BeanDefinition definition = null;
			String beanName = null;
			if (value instanceof BeanDefinition beanDefinition) {
				definition = beanDefinition;
				beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
			}
			else if (value instanceof BeanDefinitionHolder holder) {
				definition = holder.getBeanDefinition();
				beanName = holder.getBeanName();
			}

			if (definition != null) {
				boolean nestedScoped = scope.equals(definition.getScope());
				boolean scopeChangeRequiresProxy = !scoped && nestedScoped;
				if (scopeChangeRequiresProxy) {
					// Exit here so that nested inner bean definitions are not
					// analysed
					return createScopedProxy(beanName, definition, registry, proxyTargetClass);
				}
			}

			// Nested inner bean definitions are recursively analysed here
			value = super.resolveValue(value);
			return value;

		}

	}

}
