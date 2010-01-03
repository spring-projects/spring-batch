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

import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

/**
 * {@link ApplicationContextFactory} that can be used to load a context from an
 * XML location in a bundle.
 * 
 * @author Dave Syer
 * 
 */
public class OsgiBundleXmlApplicationContextFactory implements BundleContextAware, ApplicationContextFactory,
		ApplicationContextAware {

	private BundleContext bundleContext;

	private ApplicationContext parent;

	private String path;

	private String displayName;

	/**
	 * @param path the resource path to the xml to load for the child context.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @param displayName the display name for the application context created.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Setter for the parent application context.
	 * 
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		parent = applicationContext;
	}

	/**
	 * Stash the {@link BundleContext} for creating a job application context
	 * later.
	 * 
	 * @see org.springframework.osgi.context.BundleContextAware#setBundleContext(org.osgi.framework.BundleContext)
	 */
	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/**
	 * Create an application context from the provided path, using the current
	 * OSGi {@link BundleContext} and the enclosing Spring
	 * {@link ApplicationContext} as a parent context.
	 * 
	 * @see ApplicationContextFactory#createApplicationContext()
	 */
	public ConfigurableApplicationContext createApplicationContext() {
		OsgiBundleXmlApplicationContext context = new OsgiBundleXmlApplicationContext(new String[] { path }, parent);
		String displayName = bundleContext.getBundle().getSymbolicName() + ":" + this.displayName;
		context.setDisplayName(displayName);
		context.setBundleContext(bundleContext);
		context.refresh();
		return context;
	}

	@Override
	public String toString() {
		String bundleId = bundleContext == null ? null : (bundleContext.getBundle() == null ? bundleContext.toString()
				: "" + bundleContext.getBundle().getBundleId());
		return "OsgiBundleXmlApplicationContext [path=" + path + ", bundle=" + bundleId + "]";
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return toString().equals(obj.toString());
	}

}
