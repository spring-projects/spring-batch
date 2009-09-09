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
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link ListableJobLocator} interface that assumes all
 * Jobs will be loaded from class path xml resources. Each resource provided is
 * loaded as an application context with the current context as its parent, and
 * then all the jobs from the child context are registered under their bean
 * names. A {@link JobRegistry} is required, but if there is a unique one
 * available in the current application context, then that will be used by
 * default.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @since 2.0
 * @since 2.1 this class does not implement {@link JobRegistry}
 */
public class ClassPathXmlJobLoader implements ApplicationContextAware, InitializingBean, DisposableBean,
		ApplicationListener {

	private static Log logger = LogFactory.getLog(ClassPathXmlJobLoader.class);

	private List<Resource> jobPaths;

	private ApplicationContext parent;

	private JobRegistry jobRegistry;

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

	/**
	 * The {@link JobRegistry} to use for jobs created. If not provided an
	 * instance will be discovered from the application context.
	 * 
	 * @param jobRegistry
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
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

	/**
	 * Initialize the {@link JobRegistry} if not already injected. Attempts to
	 * discover a registry from the application context, searching for a unique
	 * bean of type {@link JobRegistry}.
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {

		if (jobRegistry == null) {
			String[] names = parent.getBeanNamesForType(JobRegistry.class);
			Assert.state(names.length == 1, "Precisely one bean of type JobRegistry is required.  Found = "
					+ names.length);
			jobRegistry = (JobRegistry) parent.getBean(names[0]);
		}

	}

	/**
	 * Create all the application contexts required and set up job registry
	 * entries with all the instances of {@link Job} found therein.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public final void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent && event.getSource() == parent) {
			try {
				initialize();
			}
			catch (DuplicateJobException e) {
				throw new IllegalStateException(e);
			}
			catch (NoSuchJobException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * Create jobs as instructed and register them so they can be accessed via
	 * the {@link JobLocator} interface. Normally called from
	 * {@link #onApplicationEvent(ApplicationEvent)} when the parent context is
	 * refreshed.
	 * 
	 * @throws DuplicateJobException if the job registry detects a duplicate job
	 * @throws NoSuchJobException if no jobs are registered, since this is
	 * usually an error
	 */
	protected void initialize() throws DuplicateJobException, NoSuchJobException {

		for (Resource resource : jobPaths) {

			ConfigurableApplicationContext context = createApplicationContext(parent, resource);
			contexts.add(context);
			String[] names = context.getBeanNamesForType(Job.class);

			for (String name : names) {
				logger.debug("Registering job: " + name + " from context: " + resource);
				JobFactory jobFactory = new ReferenceJobFactory((Job) context.getBean(name));
				jobRegistry.register(jobFactory);
			}

		}

		if (jobRegistry.getJobNames().isEmpty()) {
			throw new NoSuchJobException("Could not locate any jobs in resources provided.");
		}

	}

	/**
	 * Create an application context from the resource provided. Extension point
	 * for subclasses if they need to customize the context in any way. The
	 * default uses a {@link ClassPathXmlApplicationContextFactory}.
	 * 
	 * @param parent the parent application context (or null if there is none)
	 * @param resource the location of the XML configuration
	 * 
	 * @return an application context containing jobs
	 */
	protected ConfigurableApplicationContext createApplicationContext(ApplicationContext parent, Resource resource) {
		ClassPathXmlApplicationContextFactory applicationContextFactory = new ClassPathXmlApplicationContextFactory();
		applicationContextFactory.setPath(resource);
		if (parent != null) {
			applicationContextFactory.setApplicationContext(parent);
		}
		return applicationContextFactory.createApplicationContext();
	}

	/**
	 * Close the contexts that were created in {@link #afterPropertiesSet()}.
	 * 
	 * @see DisposableBean#destroy()
	 */
	public void destroy() throws Exception {

		for (ConfigurableApplicationContext context : contexts) {
			if (context.isActive()) {
				context.close();
			}
		}
		for (String jobName : jobRegistry.getJobNames()) {
			jobRegistry.unregister(jobName);
		}
		contexts.clear();

	}

}
