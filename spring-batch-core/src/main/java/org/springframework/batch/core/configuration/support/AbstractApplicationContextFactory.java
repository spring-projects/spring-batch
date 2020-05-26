/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.core.configuration.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationContextFactory} implementation that takes a parent context and a path to the context to create.
 * When createApplicationContext method is called, the child {@link ApplicationContext} will be returned. The child
 * context is not re-created every time it is requested, it is lazily initialized and cached. Clients should ensure that
 * it is closed when it is no longer needed. If a path is not set, the parent will always be returned.
 *
 */
public abstract class AbstractApplicationContextFactory implements ApplicationContextFactory, ApplicationContextAware {

	private static final Log logger = LogFactory.getLog(AbstractApplicationContextFactory.class);

	private Object[] resources;

	private ConfigurableApplicationContext parent;

	private boolean copyConfiguration = true;

	private Collection<Class<? extends BeanFactoryPostProcessor>> beanFactoryPostProcessorClasses;

	private Collection<Class<?>> beanPostProcessorExcludeClasses;

	/**
	 * Create a factory instance with the resource specified. The resources are Spring configuration files or java
	 * packages containing configuration files.
	 *
	 * @param resource resource to be used in the creation of the ApplicationContext.
	 */
	public AbstractApplicationContextFactory(Object... resource) {

		this.resources = resource;
		beanFactoryPostProcessorClasses = new ArrayList<>();
		beanFactoryPostProcessorClasses.add(PropertyPlaceholderConfigurer.class);
		beanFactoryPostProcessorClasses.add(PropertySourcesPlaceholderConfigurer.class);
		beanFactoryPostProcessorClasses.add(CustomEditorConfigurer.class);
		beanPostProcessorExcludeClasses = new ArrayList<>();
		/*
		 * Assume that a BeanPostProcessor that is BeanFactoryAware must be specific to the parent and remove it from
		 * the child (e.g. an AutoProxyCreator will not work properly). Unfortunately there might still be a a
		 * BeanPostProcessor with a dependency that itself is BeanFactoryAware, but we can't legislate for that here.
		 */
		beanPostProcessorExcludeClasses.add(BeanFactoryAware.class);
	}

	/**
	 * Flag to indicate that configuration such as bean post processors and custom editors should be copied from the
	 * parent context. Defaults to true.
	 *
	 * @param copyConfiguration the flag value to set
	 */
	public void setCopyConfiguration(boolean copyConfiguration) {
		this.copyConfiguration = copyConfiguration;
	}

	/**
	 * Protected access for subclasses to the flag determining whether configuration should be copied from parent
	 * context.
	 *
	 * @return the flag value
	 */
	protected final boolean isCopyConfiguration() {
		return copyConfiguration;
	}

	/**
	 * Determines which bean factory post processors (like property placeholders) should be copied from the parent
	 * context. Defaults to {@link PropertyPlaceholderConfigurer} and {@link CustomEditorConfigurer}.
	 *
	 * @param beanFactoryPostProcessorClasses array of post processor types to be copied
	 */

	public void setBeanFactoryPostProcessorClasses(
			Class<? extends BeanFactoryPostProcessor>[] beanFactoryPostProcessorClasses) {
		this.beanFactoryPostProcessorClasses = new ArrayList<>();
		for (int i = 0; i < beanFactoryPostProcessorClasses.length; i++) {
			this.beanFactoryPostProcessorClasses.add(beanFactoryPostProcessorClasses[i]);
		}
	}

	/**
	 * Determines by exclusion which bean post processors should be copied from the parent context. Defaults to
	 * {@link BeanFactoryAware} (so any post processors that have a reference to the parent bean factory are not copied
	 * into the child). Note that these classes do not themselves have to be {@link BeanPostProcessor} implementations
	 * or sub-interfaces.
	 *
	 * @param beanPostProcessorExcludeClasses the classes to set
	 */
	public void setBeanPostProcessorExcludeClasses(Class<?>[] beanPostProcessorExcludeClasses) {
		this.beanPostProcessorExcludeClasses = new ArrayList<>();
		for (int i = 0; i < beanPostProcessorExcludeClasses.length; i++) {
			this.beanPostProcessorExcludeClasses.add(beanPostProcessorExcludeClasses[i]);
		}

	}

	/**
	 * Protected access to the list of bean factory post processor classes that should be copied over to the context
	 * from the parent.
	 *
	 * @return the classes for post processors that were nominated for copying
	 */
	protected final Collection<Class<? extends BeanFactoryPostProcessor>> getBeanFactoryPostProcessorClasses() {
		return beanFactoryPostProcessorClasses;
	}

	/**
	 * Setter for the parent application context.
	 *
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext == null) {
			return;
		}
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext);
		parent = (ConfigurableApplicationContext) applicationContext;
	}

	/**
	 * Creates an {@link ApplicationContext} from the provided path.
	 *
	 * @see ApplicationContextFactory#createApplicationContext()
	 */
	@Override
	public ConfigurableApplicationContext createApplicationContext() {

		if (resources == null || resources.length == 0) {
			return parent;
		}

		return createApplicationContext(parent, resources);

	}

	protected abstract ConfigurableApplicationContext createApplicationContext(ConfigurableApplicationContext parent,
			Object... resources);

	/**
	 * Extension point for special subclasses that want to do more complex things with the context prior to refresh. The
	 * default implementation does nothing.
	 *
	 * @param parent the parent for the new application context
	 * @param context the new application context before it is refreshed, but after bean factory is initialized
	 *
	 * @see AbstractApplicationContextFactory#setBeanFactoryPostProcessorClasses(Class[])
	 */
	protected void prepareContext(ConfigurableApplicationContext parent, ConfigurableApplicationContext context) {
	}

	/**
	 * Extension point for special subclasses that want to do more complex things with the bean factory prior to
	 * refresh. The default implementation copies all configuration from the parent according to the
	 * {@link #setCopyConfiguration(boolean) flag} set.
	 *
	 * @param parent the parent bean factory for the new context (will never be null)
	 * @param beanFactory the new bean factory before bean definitions are loaded
	 *
	 * @see AbstractApplicationContextFactory#setCopyConfiguration(boolean)
	 * @see DefaultListableBeanFactory#copyConfigurationFrom(ConfigurableBeanFactory)
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory parent,
			ConfigurableListableBeanFactory beanFactory) {
		if (copyConfiguration && parent != null) {
			List<BeanPostProcessor> parentPostProcessors = new ArrayList<>();
			List<BeanPostProcessor> childPostProcessors = new ArrayList<>();

			childPostProcessors.addAll(beanFactory instanceof AbstractBeanFactory ? ((AbstractBeanFactory) beanFactory)
					.getBeanPostProcessors() : new ArrayList<>());
			parentPostProcessors.addAll(parent instanceof AbstractBeanFactory ? ((AbstractBeanFactory) parent)
					.getBeanPostProcessors() : new ArrayList<>());

			try {
				Class<?> applicationContextAwareProcessorClass =
						ClassUtils.forName("org.springframework.context.support.ApplicationContextAwareProcessor",
								parent.getBeanClassLoader());

				for (BeanPostProcessor beanPostProcessor : new ArrayList<>(parentPostProcessors)) {
					if (applicationContextAwareProcessorClass.isAssignableFrom(beanPostProcessor.getClass())) {
						logger.debug("Removing parent ApplicationContextAwareProcessor");
						parentPostProcessors.remove(beanPostProcessor);
					}
				}
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}

			List<BeanPostProcessor> aggregatedPostProcessors = new ArrayList<>();
			aggregatedPostProcessors.addAll(childPostProcessors);
			aggregatedPostProcessors.addAll(parentPostProcessors);

			for (BeanPostProcessor beanPostProcessor : new ArrayList<>(aggregatedPostProcessors)) {
				for (Class<?> cls : beanPostProcessorExcludeClasses) {
					if (cls.isAssignableFrom(beanPostProcessor.getClass())) {
						if (logger.isDebugEnabled()) {
							logger.debug("Removing bean post processor: " + beanPostProcessor + " of type " + cls);
						}
						aggregatedPostProcessors.remove(beanPostProcessor);
					}
				}
			}

			beanFactory.copyConfigurationFrom(parent);

			List<BeanPostProcessor> beanPostProcessors = beanFactory instanceof AbstractBeanFactory ? ((AbstractBeanFactory) beanFactory)
					.getBeanPostProcessors() : new ArrayList<>();

			beanPostProcessors.clear();
			beanPostProcessors.addAll(aggregatedPostProcessors);
		}
	}

	@Override
	public String toString() {
		return "ApplicationContextFactory [resources=" + Arrays.toString(resources) + "]";
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		return toString().equals(obj.toString());
	}

}
