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
package org.springframework.batch.core.scope;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.batch.core.scope.util.ContextFactory;
import org.springframework.batch.core.scope.util.PlaceholderProxyFactoryBean;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * ScopeSupport.
 * 
 * @author Dave Syer
 */
public abstract class ScopeSupport implements Scope, BeanFactoryPostProcessor, Ordered {

	private static boolean springThreeDetected;

	private static boolean cachedSpringThreeResult;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private String name;

	private boolean proxyTargetClass = false;

	private ContextFactory contextFactory;

	/**
	 * ScopeSupport constructor.
	 * 
	 * @param defaultScopeName the default scope name
	 * @param contextFactory the ContextFactory
	 */
	protected ScopeSupport(String defaultScopeName, ContextFactory contextFactory) {
		this.name = defaultScopeName;
		this.contextFactory = contextFactory;
	}

	/**
	 * Flag to indicate that proxies should use dynamic subclassing. This allows
	 * classes with no interface to be proxied. Defaults to false.
	 * 
	 * @param proxyTargetClass set to true to have proxies created using dynamic
	 *        subclasses
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * @param order the order value to set priority of callback execution for
	 *        the {@link BeanFactoryPostProcessor} part of this scope bean.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	/**
	 * Public setter for the name property. This can then be used as a bean
	 * definition attribute, e.g. scope="step".
	 * 
	 * @param name the name to set for this scope.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the scope name.
	 * 
	 * @return the scope name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Register this scope with the enclosing BeanFactory.
	 * 
	 * @see BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)
	 * 
	 * @param beanFactory the BeanFactory to register with
	 * @throws BeansException if there is a problem.
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		beanFactory.registerScope(name, this);

		Assert.state(beanFactory instanceof BeanDefinitionRegistry,
				"BeanFactory was not a BeanDefinitionRegistry, so scope cannot be used.");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			// Replace this or any of its inner beans with scoped proxy if it
			// has this scope
			boolean scoped = name.equals(definition.getScope());
			Scopifier scopifier = new Scopifier(registry, name, proxyTargetClass, scoped, contextFactory);
			scopifier.visitBeanDefinition(definition);
			if (scoped) {
				if (!isSpringThree()) {
					new ExpressionHider(name, scoped).visitBeanDefinition(definition);
				}
				createScopedProxy(beanName, definition, registry, proxyTargetClass, contextFactory);
			}
		}

	}

	private static boolean isSpringThree() {
		if (!cachedSpringThreeResult) {
			springThreeDetected = ReflectionUtils.findMethod(Scope.class, "resolveContextualObject",
					new Class<?>[] { String.class }) != null;
			cachedSpringThreeResult = true;
		}
		return springThreeDetected;
	}

	/**
	 * Wrap a target bean definition in a proxy that defers initialization until
	 * after the context is available. Amounts to adding
	 * &lt;aop-auto-proxy/&gt; to a scoped bean. Also if Spring EL is not
	 * available will enable a weak version of late binding as described in the
	 * class-level docs.
	 * 
	 * @param beanName the bean name to replace
	 * @param definition the bean definition to replace
	 * @param registry the enclosing {@link BeanDefinitionRegistry}
	 * @param proxyTargetClass true if we need to force use of dynamic
	 *        subclasses
	 * @param contextFactory the ContextFactory
	 * @return a {@link BeanDefinitionHolder} for the new representation of the
	 *         target. Caller should register it if needed to be visible at top level in
	 *         bean factory.
	 */
	private static BeanDefinitionHolder createScopedProxy(String beanName, BeanDefinition definition,
			BeanDefinitionRegistry registry, boolean proxyTargetClass, ContextFactory contextFactory) {

		BeanDefinitionHolder proxyHolder;

		if (isSpringThree()) {
			proxyHolder = ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(definition, beanName), registry,
					proxyTargetClass);
		}
		else {
			// Create the scoped proxy...
			proxyHolder = PlaceholderProxyFactoryBean.createScopedProxy(new BeanDefinitionHolder(definition, beanName),
					registry, proxyTargetClass, contextFactory);
			// ...and register it under the original target name
		}
		registry.registerBeanDefinition(beanName, proxyHolder.getBeanDefinition());

		return proxyHolder;

	}

	/**
	 * Helper class to scan a bean definition hierarchy and force the use of
	 * auto-proxy for scoped beans.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class Scopifier extends BeanDefinitionVisitor {

		private final boolean proxyTargetClass;

		private final BeanDefinitionRegistry registry;

		private final String scope;

		private final boolean scoped;

		private final ContextFactory contextFactory;

		public Scopifier(BeanDefinitionRegistry registry, String scope, boolean proxyTargetClass, boolean scoped,
				ContextFactory contextFactory) {
			super(new StringValueResolver() {
				public String resolveStringValue(String value) {
					return value;
				}
			});
			this.registry = registry;
			this.proxyTargetClass = proxyTargetClass;
			this.scope = scope;
			this.scoped = scoped;
			this.contextFactory = contextFactory;
		}

		@Override
		protected Object resolveValue(Object value) {

			BeanDefinition definition = null;
			String beanName = null;
			if (value instanceof BeanDefinition) {
				definition = (BeanDefinition) value;
				beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
			}
			else if (value instanceof BeanDefinitionHolder) {
				BeanDefinitionHolder holder = (BeanDefinitionHolder) value;
				definition = holder.getBeanDefinition();
				beanName = holder.getBeanName();
			}

			if (definition != null) {
				boolean nestedScoped = scope.equals(definition.getScope());
				boolean scopeChangeRequiresProxy = !scoped && nestedScoped;
				if (!isSpringThree()) {
					new ExpressionHider(scope, nestedScoped).visitBeanDefinition(definition);
				}
				if (scopeChangeRequiresProxy) {
					// Exit here so that nested inner bean definitions are not
					// analysed
					return createScopedProxy(beanName, definition, registry, proxyTargetClass, contextFactory);
				}
			}

			// Nested inner bean definitions are recursively analysed here
			value = super.resolveValue(value);
			return value;

		}

	}

	/**
	 * Helper class to scan a bean definition hierarchy and hide placeholders
	 * from Spring EL.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ExpressionHider extends BeanDefinitionVisitor {

		private static final String PLACEHOLDER_PREFIX = "#{";

		private static final String PLACEHOLDER_SUFFIX = "}";

		private static final String REPLACEMENT_PREFIX = "%{";

		private final String scope;

		private final boolean scoped;

		private ExpressionHider(String scope, final boolean scoped) {
			super(new StringValueResolver() {
				public String resolveStringValue(String value) {
					if (scoped && value.contains(PLACEHOLDER_PREFIX) && value.contains(PLACEHOLDER_SUFFIX)) {
						value = value.replace(PLACEHOLDER_PREFIX, REPLACEMENT_PREFIX);
					}
					return value;
				}
			});
			this.scope = scope;
			this.scoped = scoped;
		}

		@Override
		protected Object resolveValue(Object value) {
			BeanDefinition definition = null;
			if (value instanceof BeanDefinition) {
				definition = (BeanDefinition) value;
			}
			else if (value instanceof BeanDefinitionHolder) {
				BeanDefinitionHolder holder = (BeanDefinitionHolder) value;
				definition = holder.getBeanDefinition();
			}
			if (definition != null) {
				String otherScope = definition.getScope();
				boolean scopeChange = !scope.equals(otherScope);
				if (scopeChange) {
					new ExpressionHider(otherScope == null ? scope : otherScope, !scoped)
							.visitBeanDefinition(definition);
					// Exit here so that nested inner bean definitions are not
					// analysed by both visitors
					return value;
				}
			}
			// Nested inner bean definitions are recursively analysed here
			value = super.resolveValue(value);
			return value;
		}

	}

}
