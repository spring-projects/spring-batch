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

package org.springframework.batch.core.configuration.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextFactory} implementation that takes a parent context
 * and a path to the context to create. When createApplicationContext method is
 * called, the child {@link ApplicationContext} will be returned. The child
 * context is not re-created every time it is requested, it is lazily
 * initialized and cached. Clients should ensure that it is closed when it is no
 * longer needed. If a path is not set, the parent will always be returned.
 * 
 */
public class ClassPathXmlApplicationContextFactory implements ApplicationContextFactory, ApplicationContextAware {

	private ConfigurableApplicationContext parent;

	private Resource path;

	private ResourceXmlApplicationContext context;

	private boolean copyConfiguration = true;

	private boolean copyBeanFactoryPostProcessors = true;

	private final Object lock = new Object();

	/**
	 * Setter for the path to the xml to load to create an
	 * {@link ApplicationContext}. Use imports to centralise the configuration
	 * in one file.
	 * 
	 * @param path the resource path to the xml to load for the child context.
	 */
	public void setPath(Resource path) {
		this.path = path;
	}

	/**
	 * Flag to indicate that configuration such as bean post processors and
	 * custom editors should be copied from the parent context. Defaults to
	 * true;
	 * 
	 * @param copyConfiguration the flag value to set
	 */
	public void setCopyConfiguration(boolean copyConfiguration) {
		this.copyConfiguration = copyConfiguration;
	}

	/**
	 * Flag to indicate that bean factory post processors (like property
	 * placeholders) should be copied from the parent context. Defaults to true;
	 * 
	 * @param copyBeanFactoryPostProcessors the flag value to set
	 */
	public void setCopyBeanFactoryPostProcessors(boolean copyBeanFactoryPostProcessors) {
		this.copyBeanFactoryPostProcessors = copyBeanFactoryPostProcessors;
	}

	/**
	 * Setter for the parent application context.
	 * 
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext);
		parent = (ConfigurableApplicationContext) applicationContext;
	}

	/**
	 * Creates an {@link ApplicationContext} from the provided path.
	 * 
	 * @see ApplicationContextFactory#createApplicationContext()
	 */
	public ConfigurableApplicationContext createApplicationContext() {

		if (path == null) {
			return parent;
		}

		if (context == null) {
			// Lazy initialization of cached context
			synchronized (lock) {
				if (context == null) {
					context = new ResourceXmlApplicationContext(parent);
				}
			}
		}
		return context;

	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class ResourceXmlApplicationContext extends AbstractXmlApplicationContext {

		private final DefaultListableBeanFactory parentBeanFactory;

		/**
		 * @param parent
		 */
		public ResourceXmlApplicationContext(ConfigurableApplicationContext parent) {
			super(parent);
			if (parent != null) {
				Assert.isTrue(parent.getBeanFactory() instanceof DefaultListableBeanFactory,
						"The parent application context must have a bean factory of type DefaultListableBeanFactory");
				parentBeanFactory = (DefaultListableBeanFactory) parent.getBeanFactory();
				refreshBeanFactory();
				prepareContext(parent, this);
			}
			else {
				parentBeanFactory = null;
			}
			refresh();
		}

		@Override
		protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
			super.customizeBeanFactory(beanFactory);
			if (parentBeanFactory != null) {
				ClassPathXmlApplicationContextFactory.this.prepareBeanFactory(parentBeanFactory, beanFactory);
			}
		}

		@Override
		protected Resource[] getConfigResources() {
			return new Resource[] { path };
		}

	}

	/**
	 * Extension point for special subclasses that want to do more complex
	 * things with the context prior to refresh. The default implementation
	 * copies bean factory post processors according to the flag set. The bean
	 * factory for the context will be available if needed through
	 * {@link ConfigurableApplicationContext#getBeanFactory()
	 * context.getBeanFactory()}.
	 * 
	 * @param parent the parent for the new application context
	 * @param context the new application context before it is refreshed, but
	 * after bean factory is initialized
	 * 
	 * @see ClassPathXmlApplicationContextFactory#setCopyBeanFactoryPostProcessors(boolean)
	 */
	protected void prepareContext(ConfigurableApplicationContext parent, ConfigurableApplicationContext context) {
		if (copyBeanFactoryPostProcessors) {
			for (String name : parent.getBeanNamesForType(BeanFactoryPostProcessor.class)) {
				context.addBeanFactoryPostProcessor((BeanFactoryPostProcessor) parent.getBean(name));
			}
		}
	}

	/**
	 * Extension point for special subclasses that want to do more complex
	 * things with the bean factory prior to refresh. The default implementation
	 * copies all configuration from the parent according to the flag set.
	 * 
	 * @param parent the parent bean factory for the new context (will never be
	 * null)
	 * @param beanFactory the new bean factory before bean definitions are
	 * loaded
	 * 
	 * @see ClassPathXmlApplicationContextFactory#setCopyConfiguration(boolean)
	 * @see DefaultListableBeanFactory#copyConfigurationFrom(ConfigurableBeanFactory)
	 */
	protected void prepareBeanFactory(DefaultListableBeanFactory parent, DefaultListableBeanFactory beanFactory) {
		if (copyConfiguration && parent != null) {
			beanFactory.copyConfigurationFrom(parent);
		}
	}

}
