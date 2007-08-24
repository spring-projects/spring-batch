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

package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @since 2.1
 */
public class BatchCommandLineLauncher {

	/**
	 * The key for the parent context.
	 */
	public static final String PARENT_KEY = "simple-container";

	private ConfigurableApplicationContext parent;

	private JobLauncher launcher;

	/**
	 * Default constructor for the launcher. Sets up the parent context to use
	 * for all job executions using a context key {@link #PARENT_KEY}.
	 */
	public BatchCommandLineLauncher() {
		parent = (ConfigurableApplicationContext) ContextSingletonBeanFactoryLocator
				.getInstance().useBeanFactory(PARENT_KEY).getFactory();
	}

	/**
	 * Injection setter for the {@link JobLauncher}.
	 * 
	 * @param launcher
	 *            the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}

	/**
	 * @param path
	 *            the path to a Spring context configuration for this job
	 * @param jobName
	 *            the name of the job execution to use
	 * @throws NoSuchJobConfigurationException
	 */
	private void start(String path, String jobName)
			throws NoSuchJobConfigurationException {
		if (!path.endsWith(".xml")) {
			path = path + ".xml";
		}
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { path }, parent);
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		try {
			if (!launcher.isRunning()) {
				if (jobName == null) {
					launcher.run();
				} else {
					launcher.run(jobName);
				}
			}
		} finally {
			try {
				context.stop();
			} finally {
				context.close();
			}
		}
	}

	/**
	 * Launch a batch job using a {@link BatchCommandLineLauncher}. Creates a
	 * new Spring context for the job execution, and uses a common parent for
	 * all such contexts.
	 * 
	 * @param args
	 *            <ol>
	 *            <li> path to resource to load job configuration context
	 *            (default "job-configuration.xml");</li>
	 *            <li>runtime name for job execution (default
	 *            "job-execution-id").</li>
	 *            </ol>
	 * @throws NoSuchJobConfigurationException
	 */
	public static void main(String[] args)
			throws NoSuchJobConfigurationException {
		String path = "job-configuration.xml";
		String name = null;
		if (args.length > 0) {
			path = args[0];
		}
		if (args.length > 1) {
			name = args[1];
		}
		BatchCommandLineLauncher command = new BatchCommandLineLauncher();
		command.start(path, name);
	}

}
