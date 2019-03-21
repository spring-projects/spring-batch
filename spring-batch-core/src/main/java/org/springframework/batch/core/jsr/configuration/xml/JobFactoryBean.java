/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.listener.JobListener;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.jsr.JobListenerAdapter;
import org.springframework.batch.core.jsr.job.flow.JsrFlowJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This {@link FactoryBean} is used by the JSR-352 namespace parser to create
 * {@link FlowJob} objects. It stores all of the properties that are
 * configurable on the &lt;job/&gt;.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JobFactoryBean implements SmartFactoryBean<FlowJob> {

	private String name;

	private Boolean restartable;

	private JobRepository jobRepository;

	private JobParametersValidator jobParametersValidator;

	private JobExecutionListener[] jobExecutionListeners;

	private JobParametersIncrementer jobParametersIncrementer;

	private Flow flow;

	private JobExplorer jobExplorer;

	public JobFactoryBean(String name) {
		this.name = name;
	}

	@Override
	public final FlowJob getObject() throws Exception {
		Assert.isTrue(StringUtils.hasText(name), "The job must have an id.");
		JsrFlowJob flowJob = new JsrFlowJob(name);
		flowJob.setJobExplorer(jobExplorer);

		if (restartable != null) {
			flowJob.setRestartable(restartable);
		}

		if (jobRepository != null) {
			flowJob.setJobRepository(jobRepository);
		}

		if (jobParametersValidator != null) {
			flowJob.setJobParametersValidator(jobParametersValidator);
		}

		if (jobExecutionListeners != null) {
			flowJob.setJobExecutionListeners(jobExecutionListeners);
		}

		if (jobParametersIncrementer != null) {
			flowJob.setJobParametersIncrementer(jobParametersIncrementer);
		}

		if (flow != null) {
			flowJob.setFlow(flow);
		}

		flowJob.afterPropertiesSet();
		return flowJob;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	public void setRestartable(Boolean restartable) {
		this.restartable = restartable;
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	public void setJobParametersValidator(JobParametersValidator jobParametersValidator) {
		this.jobParametersValidator = jobParametersValidator;
	}

	public JobRepository getJobRepository() {
		return this.jobRepository;
	}

	public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
		this.jobParametersIncrementer = jobParametersIncrementer;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	@Override
	public Class<FlowJob> getObjectType() {
		return FlowJob.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public boolean isEagerInit() {
		return true;
	}

	@Override
	public boolean isPrototype() {
		return false;
	}

	/**
	 * Addresses wrapping {@link JobListener} as needed to be used with
	 * the framework.
	 *
	 * @param jobListeners a list of all job listeners
	 */
	public void setJobExecutionListeners(Object[] jobListeners) {
		if(jobListeners != null) {
			JobExecutionListener[] listeners = new JobExecutionListener[jobListeners.length];

			for(int i = 0; i < jobListeners.length; i++) {
				Object curListener = jobListeners[i];
				if(curListener instanceof JobExecutionListener) {
					listeners[i] = (JobExecutionListener) curListener;
				} else if(curListener instanceof JobListener){
					listeners[i] = new JobListenerAdapter((JobListener) curListener);
				}
			}

			this.jobExecutionListeners = listeners;
		}
	}
}
