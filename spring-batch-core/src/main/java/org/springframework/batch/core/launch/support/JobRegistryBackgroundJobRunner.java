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
package org.springframework.batch.core.launch.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ClassPathXmlApplicationContextFactory;
import org.springframework.batch.core.configuration.support.DefaultJobLoader;
import org.springframework.batch.core.configuration.support.JobLoader;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * <p>
 * Command line launcher for registering jobs with a {@link JobRegistry}.
 * Normally this will be used in conjunction with an external trigger for the
 * jobs registered, e.g. a JMX MBean wrapper for a {@link JobLauncher}, or a
 * Quartz trigger.
 * </p>
 * 
 * <p>
 * With any launch of a batch job within Spring Batch, a Spring context
 * containing the {@link Job} has to be created. Using this launcher, the jobs
 * are all registered with a {@link JobRegistry} defined in a parent application
 * context. The jobs are then set up in child contexts. All dependencies of the
 * runner will then be satisfied by autowiring by type from the parent
 * application context. Default values are provided for all fields except the
 * {@link JobRegistry}. Therefore, if autowiring fails to set it then an
 * exception will be thrown.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public class JobRegistryBackgroundJobRunner {

	/**
	 * System property key that switches the runner to "embedded" mode
	 * (returning immediately from the main method). Useful for testing
	 * purposes.
	 */
	public static final String EMBEDDED = JobRegistryBackgroundJobRunner.class.getSimpleName() + ".EMBEDDED";

	private static Log logger = LogFactory.getLog(JobRegistryBackgroundJobRunner.class);

	private JobLoader jobLoader;

	private ApplicationContext parentContext = null;

	public static boolean testing = false;

	final private String parentContextPath;

	private JobRegistry jobRegistry;

	private static List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());

	/**
	 * @param parentContextPath
	 */
	public JobRegistryBackgroundJobRunner(String parentContextPath) {
		super();
		this.parentContextPath = parentContextPath;
	}

	/**
	 * A loader for the jobs that are going to be registered.
	 * 
	 * @param jobLoader the {@link JobLoader} to set
	 */
	public void setJobLoader(JobLoader jobLoader) {
		this.jobLoader = jobLoader;
	}
	
	/**
	 * A job registry that can be used to create a job loader (if none is provided).
	 * 
	 * @param jobRegistry the {@link JobRegistry} to set
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Public getter for the startup errors encountered during parent context
	 * creation.
	 * @return the errors
	 */
	public static List<Exception> getErrors() {
		synchronized (errors) {
			return new ArrayList<Exception>(errors);
		}
	}

	private void register(String[] paths) throws DuplicateJobException, IOException {

		maybeCreateJobLoader();

		for (int i = 0; i < paths.length; i++) {

			Resource[] resources = parentContext.getResources(paths[i]);

			for (int j = 0; j < resources.length; j++) {

				Resource path = resources[j];
				logger.info("Registering Job definitions from " + Arrays.toString(resources));

				ClassPathXmlApplicationContextFactory factory = new ClassPathXmlApplicationContextFactory();
				factory.setApplicationContext(parentContext);
				factory.setResource(path);
				jobLoader.load(factory);
			}

		}

	}

	/**
	 * If there is no {@link JobLoader} then try and create one from existing
	 * bean definitions.
	 */
	private void maybeCreateJobLoader() {

		if (jobLoader != null) {
			return;
		}

		String[] names = parentContext.getBeanNamesForType(JobLoader.class);
		if (names.length == 0) {
			if (parentContext.containsBean("jobLoader")) {
				jobLoader = (JobLoader) parentContext.getBean("jobLoader", JobLoader.class);
				return;
			}
			if (jobRegistry != null) {
				jobLoader = new DefaultJobLoader(jobRegistry);
				return;
			}
		}

		jobLoader = (JobLoader) parentContext.getBean(names[0], JobLoader.class);
		return;

	}

	/**
	 * Supply a list of application context locations, starting with the parent
	 * context, and followed by the children. The parent must contain a
	 * {@link JobRegistry} and the child contexts are expected to contain
	 * {@link Job} definitions, each of which will be registered wit the
	 * registry.
	 * 
	 * Example usage:
	 * 
	 * <pre>
	 * $ java -classpath ... JobRegistryBackgroundJobRunner job-registry-context.xml job1.xml job2.xml ...
	 * </pre>
	 * 
	 * The child contexts are created only when needed though the
	 * {@link JobFactory} interface (but the XML is validated on startup by
	 * using it to create a {@link BeanFactory} which is then discarded).
	 * 
	 * The parent context is created in a separate thread, and the program will
	 * pause for input in an infinite loop until the user hits any key.
	 * 
	 * @param args the context locations to use (first one is for parent)
	 * @throws Exception if anything goes wrong with the context creation
	 */
	public static void main(String... args) throws Exception {

		Assert.state(args.length >= 1, "At least one argument (the parent context path) must be provided.");

		final JobRegistryBackgroundJobRunner launcher = new JobRegistryBackgroundJobRunner(args[0]);
		errors.clear();

		logger.info("Starting job registry in parent context from XML at: [" + args[0] + "]");

		new Thread(new Runnable() {
			public void run() {
				try {
					launcher.run();
				}
				catch (RuntimeException e) {
					errors.add(e);
					throw e;
				}
			};
		}).start();

		logger.info("Waiting for parent context to start.");
		while (launcher.parentContext == null && errors.isEmpty()) {
			Thread.sleep(100L);
		}

		synchronized (errors) {
			if (!errors.isEmpty()) {
				logger.info(errors.size() + " errors detected on startup of parent context.  Rethrowing.");
				throw errors.get(0);
			}
		}
		errors.clear();

		// Paths to individual job configurations.
		final String[] paths = new String[args.length - 1];
		System.arraycopy(args, 1, paths, 0, paths.length);

		logger.info("Parent context started.  Registering jobs from paths: " + Arrays.asList(paths));
		launcher.register(paths);

		if (System.getProperty(EMBEDDED) != null) {
			launcher.destroy();
			return;
		}

		synchronized (JobRegistryBackgroundJobRunner.class) {
			System.out
					.println("Started application.  Interrupt (CTRL-C) or call JobRegistryBackgroundJobRunner.stop() to exit.");
			JobRegistryBackgroundJobRunner.class.wait();
		}
		launcher.destroy();

	}

	/**
	 * De-register all the {@link Job} instances that were regsistered by this
	 * post processor.
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	private void destroy() throws Exception {
		jobLoader.clear();
	}

	private void run() {
		final ApplicationContext parent = new ClassPathXmlApplicationContext(parentContextPath);
		parent.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		parent.getAutowireCapableBeanFactory().initializeBean(this, getClass().getSimpleName());
		this.parentContext = parent;
	}

	/**
	 * If embedded in a JVM, call this method to terminate the main method.
	 */
	public static void stop() {
		synchronized (JobRegistryBackgroundJobRunner.class) {
			JobRegistryBackgroundJobRunner.class.notify();
		}
	}

}
