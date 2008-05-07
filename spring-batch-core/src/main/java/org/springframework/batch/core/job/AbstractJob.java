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

package org.springframework.batch.core.job;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.listener.CompositeExecutionJobListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.ClassUtils;

/**
 * Batch domain object representing a job. Job is an explicit abstraction
 * representing the configuration of a job specified by a developer. It should
 * be noted that restart policy is applied to the job as a whole and not to a
 * step.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public abstract class AbstractJob implements BeanNameAware, Job {

	private List steps = new ArrayList();

	private String name;

	private boolean restartable = false;

	private JobRepository jobRepository;

	private CompositeExecutionJobListener listener = new CompositeExecutionJobListener();

	/**
	 * Default constructor.
	 */
	public AbstractJob() {
		super();
	}

	/**
	 * Convenience constructor to immediately add name (which is mandatory but
	 * not final).
	 * 
	 * @param name
	 */
	public AbstractJob(String name) {
		super();
		this.name = name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of
	 * the callbacks in a Spring container the name property will be set first
	 * if it is present. Care is needed with bean definition inheritance - if a
	 * parent bean has a name, then its children need an explicit name as well,
	 * otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * Set the name property. Always overrides the default value if this object
	 * is a Spring bean.
	 * 
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.IJob#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.IJob#getSteps()
	 */
	public List getSteps() {
		return steps;
	}

	public void setSteps(List steps) {
		this.steps.clear();
		this.steps.addAll(steps);
	}

	public void addStep(Step step) {
		this.steps.add(step);
	}

	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.IJob#isRestartable()
	 */
	public boolean isRestartable() {
		return restartable;
	}

	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

	/**
	 * Public setter for injecting {@link JobExecutionListener}s. They will all
	 * be given the listener callbacks at the appropriate point in the job.
	 * 
	 * @param listeners the listeners to set.
	 */
	public void setJobExecutionListeners(JobExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			this.listener.register(listeners[i]);
		}
	}

	/**
	 * Register a single listener for the {@link JobExecutionListener}
	 * callbacks.
	 * 
	 * @param listener a {@link JobExecutionListener}
	 */
	public void registerJobExecutionListener(JobExecutionListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Public setter for the {@link JobRepository} that is needed to manage the
	 * state of the batch meta domain (jobs, steps, executions) during the life
	 * of a job.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	protected JobRepository getJobRepository() {
		return jobRepository;
	}

	protected CompositeExecutionJobListener getCompositeListener() {
		return listener;
	}
}
