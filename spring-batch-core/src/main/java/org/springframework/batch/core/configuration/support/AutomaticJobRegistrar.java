/*
 * Copyright 2006-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Loads and unloads {@link Job Jobs} when the application context is created and destroyed. Each resource provided is
 * loaded as an application context with the current context as its parent, and then all the jobs from the child context
 * are registered under their bean names. A {@link JobRegistry} is required.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 * @since 2.1
 */
public class AutomaticJobRegistrar implements Ordered, SmartLifecycle, ApplicationContextAware,
		InitializingBean {

	private Collection<ApplicationContextFactory> applicationContextFactories = new ArrayList<>();

	private JobLoader jobLoader;

	private ApplicationContext applicationContext;

	private volatile boolean running = false;

	private int phase = Integer.MIN_VALUE + 1000;

	private boolean autoStartup = true;

	private Object lifecycleMonitor = new Object();

	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * The enclosing application context, which can be used to check if {@link ApplicationContextEvent events} come
	 * from the expected source.
	 *
	 * @param applicationContext the enclosing application context if there is one
	 * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Add some factories to the set that will be used to load contexts and jobs.
	 *
	 * @param applicationContextFactory the {@link ApplicationContextFactory} values to use
	 */
	public void addApplicationContextFactory(ApplicationContextFactory applicationContextFactory) {
		if (applicationContextFactory instanceof ApplicationContextAware) {
			((ApplicationContextAware) applicationContextFactory).setApplicationContext(applicationContext);
		}
		this.applicationContextFactories.add(applicationContextFactory);
	}

	/**
	 * Add some factories to the set that will be used to load contexts and jobs.
	 *
	 * @param applicationContextFactories the {@link ApplicationContextFactory} values to use
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

	@Override
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
	 */
	@Override
	public void afterPropertiesSet() {

		Assert.state(jobLoader != null, "A JobLoader must be provided");

	}

	/**
	 * Delegates to {@link JobLoader#clear()}.
	 *
	 * @see Lifecycle#stop()
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			jobLoader.clear();
			running = false;
		}
	}

	/**
	 * Take all the contexts from the factories provided and pass them to the {@link JobLoader}.
	 *
	 * @see Lifecycle#start()
	 */
	@Override
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
	@Override
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return running;
		}
	}

	@Override
	public boolean isAutoStartup() {
		return autoStartup;
	}

	/**
	 * @param autoStartup true for auto start.
	 * @see #isAutoStartup()
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase() {
		return phase;
	}

	/**
	 * @param phase the phase.
	 * @see #getPhase()
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

}
