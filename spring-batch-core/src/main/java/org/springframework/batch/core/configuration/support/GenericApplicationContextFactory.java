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

import java.io.IOException;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextFactory} implementation that takes a parent context and a path to the context to create.
 * When createApplicationContext method is called, the child {@link ApplicationContext} will be returned. The child
 * context is not re-created every time it is requested, it is lazily initialized and cached. Clients should ensure that
 * it is closed when it is no longer needed.
 * 
 */
public class GenericApplicationContextFactory extends AbstractApplicationContextFactory {

	/**
	 * Create an application context factory for the resource specified. The resource can be an actual {@link Resource},
	 * in which case it will be interpreted as an XML file, or it can be a &#64;Configuration class, or a package name.
	 * 
	 * @param resource a resource (XML configuration file, &#064;Configuration class or java package to scan)
	 */
	public GenericApplicationContextFactory(Object resource) {
		super(resource);
	}

	/**
	 * @see AbstractApplicationContextFactory#createApplicationContext(ConfigurableApplicationContext, Object)
	 */
	@Override
	protected ConfigurableApplicationContext createApplicationContext(ConfigurableApplicationContext parent,
			Object resource) {
		if (resource instanceof Resource) {
			return new ResourceXmlApplicationContext(parent, (Resource) resource);
		}
		if (resource instanceof Class<?>) {
			return new ResourceAnnotationApplicationContext(parent, (Class<?>) resource);
		}
		if (resource instanceof String) {
			return new ResourceAnnotationApplicationContext(parent, (String) resource);
		}
		throw new IllegalArgumentException("No application context could be created for resource type: "
				+ resource.getClass());
	}

	private abstract class ApplicationContextHelper {

		private final DefaultListableBeanFactory parentBeanFactory;

		private final ConfigurableApplicationContext parent;

		public ApplicationContextHelper(ConfigurableApplicationContext parent, GenericApplicationContext context,
				Object config) {
			this.parent = parent;
			if (parent != null) {
				Assert.isTrue(parent.getBeanFactory() instanceof DefaultListableBeanFactory,
						"The parent application context must have a bean factory of type DefaultListableBeanFactory");
				parentBeanFactory = (DefaultListableBeanFactory) parent.getBeanFactory();
			}
			else {
				parentBeanFactory = null;
			}
			context.setParent(parent);
			context.setId(generateId(config));
			loadConfiguration(config);
			prepareContext(parent, context);
		}

		protected abstract String generateId(Object config);

		protected abstract void loadConfiguration(Object config);

		protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			if (parentBeanFactory != null) {
				GenericApplicationContextFactory.this.prepareBeanFactory(parentBeanFactory, beanFactory);
				for (Class<? extends BeanFactoryPostProcessor> cls : getBeanFactoryPostProcessorClasses()) {
					for (String name : parent.getBeanNamesForType(cls)) {
						beanFactory.registerSingleton(name, ((BeanFactoryPostProcessor) parent.getBean(name)));
					}
				}
			}
		}

	}

	private final class ResourceXmlApplicationContext extends GenericXmlApplicationContext {

		private final ApplicationContextHelper helper;

		/**
		 * @param parent
		 */
		public ResourceXmlApplicationContext(ConfigurableApplicationContext parent, Resource resource) {
			helper = new ApplicationContextHelper(parent, this, resource) {
				@Override
				protected String generateId(Object config) {
					Resource resource = (Resource) config;
					try {
						return resource.getURI().toString();
					}
					catch (IOException e) {
						return resource.toString();
					}
				}
				@Override
				protected void loadConfiguration(Object config) {
					Resource resource = (Resource) config;
					load(resource);
				}
			};
			refresh();
		}

		@Override
		protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			super.prepareBeanFactory(beanFactory);
			helper.prepareBeanFactory(beanFactory);
		}

		@Override
		public String toString() {
			return "ResourceXmlApplicationContext:" + getId();
		}

	}

	private final class ResourceAnnotationApplicationContext extends AnnotationConfigApplicationContext {

		private final ApplicationContextHelper helper;

		public ResourceAnnotationApplicationContext(ConfigurableApplicationContext parent, Object resource) {
			helper = new ApplicationContextHelper(parent, this, resource) {
				@Override
				protected String generateId(Object config) {
					if (config instanceof Class) {
						Class<?> type = (Class<?>) config;
						return type.getName();
					}
					else {
						return config.toString();
					}
				}
				@Override
				protected void loadConfiguration(Object config) {
					if (config instanceof Class) {
						Class<?> type = (Class<?>) config;
						register(type);
					}
					else {
						String pkg = (String) config;
						scan(pkg);
					}
				}
			};
			refresh();
		}

		@Override
		protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			super.prepareBeanFactory(beanFactory);
			helper.prepareBeanFactory(beanFactory);
		}

		@Override
		public String toString() {
			return "ResourceAnnotationApplicationContext:" + getId();
		}

	}

}
