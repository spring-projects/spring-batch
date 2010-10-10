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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Loads and unloads {@link Job Jobs} when the application context is created
 * and destroyed. Each resource provided is loaded as an application context
 * with the current context as its parent, and then all the jobs from the child
 * context are registered under their bean names. A {@link JobRegistry} is
 * required.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class AutomaticJobRegistrar implements Ordered, Lifecycle, ApplicationListener, ApplicationContextAware, InitializingBean {

	private Collection<ApplicationContextFactory> applicationContextFactories = new ArrayList<ApplicationContextFactory>();

	private JobLoader jobLoader;

	private ApplicationContext applicationContext;

	private volatile boolean running = false;

	private Object lifecycleMonitor = new Object();
	
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * The enclosing application context, which can be used to check if
	 * {@link ApplicationEvent events} come from the expected source.
	 * 
	 * @param applicationContext the enclosing application context if there is
	 * one
	 * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Add some factories to the set that will be used to load contexts and jobs.
	 * 
	 * @param applicationContextFactories the {@link ApplicationContextFactory}
	 * values to use
	 */
	public void setApplicationContextFactories(ApplicationContextFactory[] applicationContextFactories) {
		for (ApplicationContextFactory applicationContextFactory : applicationContextFactories) {
			this.applicationContextFactories.add(applicationContextFactory);
		}
	}

	/**
	 * The job loader that will be used to load and manage jobs.
	 * 
	 * @param jobLoader the {@link JobLoader} to set
	 */
	public void setJobLoader(JobLoader jobLoader) {
		this.jobLoader = jobLoader;
	}

	public int getOrder() {
		return order;
	}

	/**
	 * The order to start up and shutdown.
	 * @param order the order (default {@link Ordered#LOWEST_PRECEDENCE}).
	 * @see Ordered
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * @throws Exception
	 */
	public void afterPropertiesSet() {

		Assert.state(jobLoader != null, "A JobLoader must be provided");

	}

	/**
	 * Creates all the application contexts required and set up job registry
	 * entries with all the instances of {@link Job} found therein. Also closes
	 * the contexts when the enclosing context is closed.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public final void onApplicationEvent(ApplicationEvent event) {
		// TODO: With Spring 3 a SmartLifecycle is started automatically
		if (event.getSource() == applicationContext) {
			if (event instanceof ContextRefreshedEvent) {
				start();
			}
			else if (event instanceof ContextClosedEvent) {
				stop();
			}
		}
	}

	/**
	 * Delegates to {@link JobLoader#clear()}.
	 * 
	 * @see Lifecycle#stop()
	 */
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			jobLoader.clear();
			running = false;
		}
	}

	/**
	 * Take all the contexts from the factories provided and pass them to teh
	 * {@link JobLoader}.
	 * 
	 * @see Lifecycle#start()
	 */
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (running) {
				return;
			}
			for (ApplicationContextFactory factory : applicationContextFactories) {
				try {
					jobLoader.load(factory);
				}
				catch (DuplicateJobException e) {
					throw new IllegalStateException(e);
				}
			}
			running = true;
		}
	}

	/**
	 * Check if this component has been started.
	 * 
	 * @return true if started successfully and not stopped
	 * @see Lifecycle#isRunning()
	 */
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return running;
		}
	}

}
