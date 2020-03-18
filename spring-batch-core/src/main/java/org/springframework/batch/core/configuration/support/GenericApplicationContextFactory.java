/*
 * Copyright 2006-2007 the original author or authors.
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	 * All types must be the same (mixing XML with a java package for example is not allowed and will result in an
	 * {@link java.lang.IllegalArgumentException}).
	 * 
	 * @param resources some resources (XML configuration files, &#064;Configuration classes or java packages to scan)
	 */
	public GenericApplicationContextFactory(Object... resources) {
		super(resources);
	}

	/**
	 * @see AbstractApplicationContextFactory#createApplicationContext(ConfigurableApplicationContext, Object...)
	 */
	@Override
	protected ConfigurableApplicationContext createApplicationContext(ConfigurableApplicationContext parent,
			Object... resources) {
		ConfigurableApplicationContext context;

		if (allObjectsOfType(resources, Resource.class)) {
			 context = new ResourceXmlApplicationContext(parent, resources);
		} else if (allObjectsOfType(resources, Class.class)) {
			 context =  new ResourceAnnotationApplicationContext(parent, resources);
		} else if (allObjectsOfType(resources, String.class)) {
			 context = new ResourceAnnotationApplicationContext(parent, resources);
		} else {
			List<Class<?>> types = new ArrayList<>();
			for (Object resource : resources) {
				types.add(resource.getClass());
			}
			throw new IllegalArgumentException("No application context could be created for resource types: "
													   + Arrays.toString(types.toArray()));
		}

		return context;
	}
	
	private boolean allObjectsOfType(Object[] objects, Class<?> type) {
		for (Object object : objects) {
			if (!type.isInstance(object)) {
				return false;
			}
		}
		return true;
	}

	private abstract class ApplicationContextHelper {

		private final DefaultListableBeanFactory parentBeanFactory;

		private final ConfigurableApplicationContext parent;

		public ApplicationContextHelper(ConfigurableApplicationContext parent, GenericApplicationContext context,
				Object... config) {
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

		protected abstract String generateId(Object... configs);

		protected abstract void loadConfiguration(Object... configs);

		protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			if (parentBeanFactory != null) {
				GenericApplicationContextFactory.this.prepareBeanFactory(parentBeanFactory, beanFactory);
				for (Class<? extends BeanFactoryPostProcessor> cls : getBeanFactoryPostProcessorClasses()) {
					for (String name : parent.getBeanNamesForType(cls)) {
						beanFactory.registerSingleton(name, (parent.getBean(name)));
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
		public ResourceXmlApplicationContext(ConfigurableApplicationContext parent, Object... resources) {

			class ResourceXmlApplicationContextHelper extends ApplicationContextHelper {

				ResourceXmlApplicationContextHelper(ConfigurableApplicationContext parent, GenericApplicationContext context, Object... config) {
					super(parent, context, config);
				}

				@Override
				protected String generateId(Object... configs) {
					Resource[] resources = Arrays.copyOfRange(configs, 0, configs.length, Resource[].class);
  					try {
 						List<String> uris = new ArrayList<>();
 						for (Resource resource : resources) {
 							uris.add(resource.getURI().toString());
 						}
 						return StringUtils.collectionToCommaDelimitedString(uris);
  					}
  					catch (IOException e) {
 						return Arrays.toString(resources);
  					}
				}
				@Override
				protected void loadConfiguration(Object... configs) {
					Resource[] resources = Arrays.copyOfRange(configs, 0, configs.length, Resource[].class);
					load(resources);
				}
			}
			helper = new ResourceXmlApplicationContextHelper(parent, this, resources);
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

		public ResourceAnnotationApplicationContext(ConfigurableApplicationContext parent, Object... resources) {

			class ResourceAnnotationApplicationContextHelper extends ApplicationContextHelper {

				public ResourceAnnotationApplicationContextHelper(ConfigurableApplicationContext parent, GenericApplicationContext context, Object... config) {
					super(parent, context, config);
				}

				@Override
				protected String generateId(Object... configs) {
					if (allObjectsOfType(configs, Class.class)) {
						Class<?>[] types = Arrays.copyOfRange(configs, 0, configs.length, Class[].class);
						List<String> names = new ArrayList<>();
						for (Class<?> type : types) {
							names.add(type.getName());
						}
						return StringUtils.collectionToCommaDelimitedString(names);
					}
					else {
						return Arrays.toString(configs);
					}
				}
				@Override
				protected void loadConfiguration(Object... configs) {
					if (allObjectsOfType(configs, Class.class)) {
						Class<?>[] types = Arrays.copyOfRange(configs, 0, configs.length, Class[].class);
						register(types);
					}
					else {
						String[] pkgs = Arrays.copyOfRange(configs, 0, configs.length, String[].class);
						scan(pkgs);
					}
				}
			}
			helper = new ResourceAnnotationApplicationContextHelper(parent, this, resources);
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
