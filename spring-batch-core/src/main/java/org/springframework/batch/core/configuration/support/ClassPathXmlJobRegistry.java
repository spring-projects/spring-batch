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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Implementation of the {@link ListableJobRegistry} interface that assumes all
 * Jobs will be loaded from class path xml resources. Each resource provided is
 * loaded as an application context with the current context as its parent, and
 * then all the jobs from the child context are registered under their bean
 * names. Care must be taken to avoid duplicate names.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.0
 */
public class ClassPathXmlJobRegistry implements ListableJobRegistry, ApplicationContextAware, InitializingBean,
		DisposableBean {

	private static Log logger = LogFactory.getLog(ClassPathXmlJobRegistry.class);

	private List<Resource> jobPaths;

	private ApplicationContext parent;

	private ListableJobRegistry jobRegistry = new MapJobRegistry();

	private Collection<ConfigurableApplicationContext> contexts = new HashSet<ConfigurableApplicationContext>();

	/**
	 * A set of resources to load. Each resource should be a Spring
	 * configuration file which is loaded into an application context whose
	 * parent is the current context. In a configuration file the resources can
	 * be given as a pattern (e.g.
	 * <code>classpath*:/config/*-job-context.xml</code>).
	 * 
	 * @param jobPaths
	 */
	public void setJobPaths(Resource[] jobPaths) {
		this.jobPaths = Arrays.asList(jobPaths);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.context.ApplicationContextAware#setApplicationContext
	 * (org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		parent = applicationContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.configuration.JobLocator#getJob(java.lang
	 * .String)
	 */
	public Job getJob(String name) throws NoSuchJobException {
		return jobRegistry.getJob(name);
	}

	/**
	 * Create all the application contexts required and set up job registry
	 * entries with all the instances of {@link Job} found therein.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {

		for (Resource resource : jobPaths) {
			ClassPathXmlApplicationContextFactory applicationContextFactory = new ClassPathXmlApplicationContextFactory();
			applicationContextFactory.setPath(resource);
			if (parent != null) {
				applicationContextFactory.setApplicationContext(parent);
			}
			ConfigurableApplicationContext context = applicationContextFactory.createApplicationContext();
			contexts.add(context);
			String[] names = context.getBeanNamesForType(Job.class);

			for (String name : names) {
				logger.debug("Registering job: " + name + " from context: " + resource);
				ApplicationContextJobFactory jobFactory = new ApplicationContextJobFactory(applicationContextFactory,
						name);
				jobRegistry.register(jobFactory);
			}
		}

		if (jobRegistry.getJobNames().isEmpty()) {
			throw new NoSuchJobException("Could not locate any jobs in resources provided.");
		}

	}

	/**
	 * Close the contexts that were created in {@link #afterPropertiesSet()}.
	 * 
	 * @see DisposableBean#destroy()
	 */
	public void destroy() throws Exception {

		try {

			for (ConfigurableApplicationContext context : contexts) {

				String[] names = context.getBeanNamesForType(Job.class);

				try {
					for (String name : names) {
						unregister(name);
					}
				}
				finally {
					context.close();
				}

			}
		}
		finally {
			contexts.clear();
		}
	}

	public Collection<String> getJobNames() {
		return jobRegistry.getJobNames();
	}

	public void register(JobFactory jobFactory) throws DuplicateJobException {
		jobRegistry.register(jobFactory);
	}

	public void unregister(String jobName) {
		jobRegistry.unregister(jobName);
	}
}
