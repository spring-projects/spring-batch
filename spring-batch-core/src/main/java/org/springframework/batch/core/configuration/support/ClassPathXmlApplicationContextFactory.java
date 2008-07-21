package org.springframework.batch.core.configuration.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

public class ClassPathXmlApplicationContextFactory implements
		ApplicationContextFactory, ApplicationContextAware {

	private ApplicationContext parent;

	private String path;

	/**
	 * @param path
	 *            the resource path to the xml to load for the child context.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Setter for the parent application context.
	 * 
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		Assert.isInstanceOf(ConfigurableApplicationContext.class,
				applicationContext);
		parent = applicationContext;
	}

	/**
	 * Creates an {@link ApplicationContext} from the provided path.
	 * 
	 * @see ApplicationContextFactory#createApplicationContext()
	 */
	public ConfigurableApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(new String[] { path }, parent);
	}

}
