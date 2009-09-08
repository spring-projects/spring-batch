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
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Implementation of the {@link ListableJobLocator} interface that assumes all
 * Jobs will be loaded from class path xml resources. Each resource provided is
 * loaded as an application context with the current context as its parent, and
 * then all the jobs from the child context are registered under their bean
 * names. Care must be taken to avoid duplicate names.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @since 2.0
 * @since 2.1 this class does not implement {@link JobRegistry}: it is a
 * {@link ListableJobLocator}
 */
public class ClassPathXmlJobRegistry implements ListableJobLocator, ApplicationContextAware, InitializingBean,
		DisposableBean {

	private static Log logger = LogFactory.getLog(ClassPathXmlJobRegistry.class);

	private List<Resource> jobPaths;

	private ApplicationContext parent;

	private JobRegistry jobRegistry = new MapJobRegistry();

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
				String groupName = getGroupName(context, resource, name);
				ApplicationContextJobFactory jobFactory = new ApplicationContextJobFactory(groupName, name,
						applicationContextFactory);
				jobRegistry.register(jobFactory);
			}
		}

		if (jobRegistry.getJobNames().isEmpty()) {
			throw new NoSuchJobException("Could not locate any jobs in resources provided.");
		}

	}

	/**
	 * Determine a group name for the job to be registered. Default
	 * implementation does nothing, but provides an extension point for
	 * specialised subclasses.
	 * 
	 * @param context the application context containing the job
	 * @param resource the resource that was used to create the context
	 * @param jobName the jobName
	 * @return a group name for the job (or null if not needed)
	 */
	protected String getGroupName(ApplicationContext context, Resource resource, String jobName) {
		return null;
	}

	/**
	 * Close the contexts that were created in {@link #afterPropertiesSet()}.
	 * 
	 * @see DisposableBean#destroy()
	 */
	public void destroy() throws Exception {

		for (ConfigurableApplicationContext context : contexts) {
			context.close();
		}
		for (String jobName : jobRegistry.getJobNames()) {
			jobRegistry.unregister(jobName);
		}
		contexts.clear();

	}

	public Collection<String> getJobNames() {
		return jobRegistry.getJobNames();
	}

}
