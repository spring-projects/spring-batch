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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class ClassPathXmlApplicationContextFactory implements ApplicationContextFactory, ApplicationContextAware {

	private ConfigurableApplicationContext parent;

	private Resource path;

	/**
	 * Setter for the path to the xml to load to create an
	 * {@link ApplicationContext}. Use imports to centralise the configuration in
	 * one file.
	 * 
	 * @param path the resource path to the xml to load for the child context.
	 */
	public void setPath(Resource path) {
		this.path = path;
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
		return new ResourceXmlApplicationContext(parent);
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class ResourceXmlApplicationContext extends AbstractXmlApplicationContext {
		/**
		 * @param parent
		 */
		private ResourceXmlApplicationContext(ApplicationContext parent) {
			super(parent);
			refresh();
		}

		protected Resource[] getConfigResources() {
			return new Resource[] {path};
		}
	}

}
