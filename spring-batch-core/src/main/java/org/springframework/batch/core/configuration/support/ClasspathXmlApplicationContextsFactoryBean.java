/*
 * Copyright 2006-2021 the original author or authors.
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
import java.util.List;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;

/**
 * A convenient factory for creating a set of {@link ApplicationContextFactory}
 * components from a set of {@link Resource resources}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class ClasspathXmlApplicationContextsFactoryBean implements FactoryBean<ApplicationContextFactory[]>, ApplicationContextAware {

	private List<Resource> resources = new ArrayList<>();

	private boolean copyConfiguration = true;

	private Class<? extends BeanFactoryPostProcessor>[] beanFactoryPostProcessorClasses;

	private Class<?>[] beanPostProcessorExcludeClasses;

	private ApplicationContext applicationContext;

	/**
	 * A set of resources to load using a
	 * {@link GenericApplicationContextFactory}. Each resource should be a
	 * Spring configuration file which is loaded into an application context
	 * whose parent is the current context. In a configuration file the
	 * resources can be given as a pattern (e.g.
	 * <code>classpath*:/config/*-context.xml</code>).
	 *
	 * @param resources array of resources to use
	 */
	public void setResources(Resource[] resources) {
		this.resources = Arrays.asList(resources);
	}

	/**
	 * Flag to indicate that configuration such as bean post processors and
	 * custom editors should be copied from the parent context. Defaults to
	 * true.
	 *
	 * @param copyConfiguration the flag value to set
	 */
	public void setCopyConfiguration(boolean copyConfiguration) {
		this.copyConfiguration = copyConfiguration;
	}

	/**
	 * Determines which bean factory post processors (like property
	 * placeholders) should be copied from the parent context. Defaults to
	 * {@link PropertySourcesPlaceholderConfigurer} and {@link CustomEditorConfigurer}.
	 *
	 * @param beanFactoryPostProcessorClasses post processor types to be copied
	 */

	public void setBeanFactoryPostProcessorClasses(
			Class<? extends BeanFactoryPostProcessor>[] beanFactoryPostProcessorClasses) {
		this.beanFactoryPostProcessorClasses = beanFactoryPostProcessorClasses;
	}

	/**
	 * Determines by exclusion which bean post processors should be copied from
	 * the parent context. Defaults to {@link BeanFactoryAware} (so any post
	 * processors that have a reference to the parent bean factory are not
	 * copied into the child). Note that these classes do not themselves have to
	 * be {@link BeanPostProcessor} implementations or sub-interfaces.
	 *
	 * @param beanPostProcessorExcludeClasses the classes to set
	 */
	public void setBeanPostProcessorExcludeClasses(Class<?>[] beanPostProcessorExcludeClasses) {
		this.beanPostProcessorExcludeClasses = beanPostProcessorExcludeClasses;
	}

	/**
	 * Create an {@link ApplicationContextFactory} from each resource provided
	 * in {@link #setResources(Resource[])}.
	 *
	 * @return an array of {@link ApplicationContextFactory}
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public ApplicationContextFactory[] getObject() throws Exception {

		if (resources == null) {
			return new ApplicationContextFactory[0];
		}

		List<ApplicationContextFactory> applicationContextFactories = new ArrayList<>();
		for (Resource resource : resources) {
			GenericApplicationContextFactory factory = new GenericApplicationContextFactory(resource);
			factory.setCopyConfiguration(copyConfiguration);
			if (beanFactoryPostProcessorClasses != null) {
				factory.setBeanFactoryPostProcessorClasses(beanFactoryPostProcessorClasses);
			}
			if (beanPostProcessorExcludeClasses != null) {
				factory.setBeanPostProcessorExcludeClasses(beanPostProcessorExcludeClasses);
			}
			factory.setApplicationContext(applicationContext);
			applicationContextFactories.add(factory);
		}
		return applicationContextFactories.toArray(new ApplicationContextFactory[applicationContextFactories.size()]);
	}

	/**
	 * The type of object returned by this factory - an array of
	 * {@link ApplicationContextFactory}.
	 *
	 * @return array of {@link ApplicationContextFactory}
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return ApplicationContextFactory[].class;
	}

	/**
	 * Optimization hint for bean factory.
	 * @return true
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * An application context that can be used as a parent context for all the
	 * factories.
	 *
	 * @param applicationContext the {@link ApplicationContext} to set
	 * @see ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
