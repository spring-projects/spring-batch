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

package org.springframework.batch.sample.quartz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.repository.DuplicateJobException;
import org.springframework.batch.sample.ClassPathXmlApplicationContextJobFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ResourceLoader;

public class QuartzBatchLauncher {

	private static Log log = LogFactory.getLog(QuartzBatchLauncher.class);

	private JobRegistry registry;

	private ResourceLoader resourceLoader;

	private ApplicationContext parentContext = null;

	/**
	 * Public setter for the {@link JobRegistry}.
	 * @param registry the registry to set
	 */
	public void setRegistry(JobRegistry registry) {
		this.registry = registry;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	private void register(String[] paths) throws DuplicateJobException {
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			ConfigurableListableBeanFactory beanFactory = new XmlBeanFactory(resourceLoader.getResource(path),
					parentContext.getAutowireCapableBeanFactory());
			String[] names = beanFactory.getBeanNamesForType(Job.class);
			for (int j = 0; j < names.length; j++) {
				registry.register(new ClassPathXmlApplicationContextJobFactory(names[j], path, parentContext));
			}
		}
	}

	public static void main(String[] args) throws Exception {

		final QuartzBatchLauncher launcher = new QuartzBatchLauncher();

		new Thread(new Runnable() {
			public void run() {
				launcher.run();
			};
		}).start();

		while (launcher.parentContext == null) {
			Thread.sleep(100L);
		}

		// Paths to individual job configurations.
		final String[] paths = new String[] { "jobs/adhocLoopJob.xml", "jobs/footballJob.xml" };

		launcher.register(paths);

		log.info("Started Quartz scheduler.");
		System.in.read();
	}

	private void run() {

		/*
		 * A simple execution environment with a Quartz scheduler. This will be
		 * used as the parent context for loading job configurations.
		 */
		final ApplicationContext parent = new ClassPathXmlApplicationContext("quartz-job-launcher-context.xml");
		parent.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		parent.getAutowireCapableBeanFactory().initializeBean(this, "quartzLauncher");
		this.parentContext = parent;

	}
}
