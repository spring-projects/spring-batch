package org.springframework.batch.core.configuration.support;

import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

public class OsgiBundleXmlApplicationContextFactory implements BundleContextAware,
		ApplicationContextFactory, ApplicationContextAware {

	private BundleContext bundleContext;

	private ApplicationContext parent;

	private String path;

	private String displayName;

	/**
	 * @param path
	 *            the resource path to the xml to load for the child context.
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
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
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
		OsgiBundleXmlApplicationContext context = new OsgiBundleXmlApplicationContext(
				new String[] { path }, parent);
		String displayName = bundleContext.getBundle().getSymbolicName() + ":" + this.displayName;
		context.setDisplayName(displayName);
		context.setBundleContext(bundleContext);
		context.refresh();
		return context;
	}

}
