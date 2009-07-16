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
import java.util.List;

import javax.print.attribute.standard.JobName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Implementation of the {@link ListableJobRegistry} interface that assumes all
 * Jobs will be loaded from ClassPathXml resources.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.0
 */
public class ClassPathXmlJobRegistry implements ListableJobRegistry, ApplicationContextAware, InitializingBean {
	
	private static Log logger = LogFactory.getLog(ClassPathXmlJobRegistry.class);

	private List<Resource> jobPaths;

	private ApplicationContext parent;

	private ListableJobRegistry jobRegistry = new MapJobRegistry();

	public void setJobPaths(Resource[] jobPaths) {
		this.jobPaths = Arrays.asList(jobPaths);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		parent = applicationContext;
	}

	public Job getJob(String name) throws NoSuchJobException {
		return jobRegistry.getJob(name);
	}

	public void afterPropertiesSet() throws Exception {

		for (Resource resource : jobPaths) {
			ClassPathXmlApplicationContextFactory applicationContextFactory = new ClassPathXmlApplicationContextFactory();
			applicationContextFactory.setPath(resource);
			applicationContextFactory.setApplicationContext(parent);
			ApplicationContext context = applicationContextFactory.createApplicationContext();
			String[] names = context.getBeanNamesForType(Job.class);

			for (String name : names) {
				logger.debug("Registering job: "+name+" from context: "+resource);
				ApplicationContextJobFactory jobFactory = new ApplicationContextJobFactory(applicationContextFactory,
						name);
				jobRegistry.register(jobFactory);
			}
		}
		
		if (jobRegistry.getJobNames().isEmpty()) {
			throw new NoSuchJobException("Could not locate any jobs in resources provided.");
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
