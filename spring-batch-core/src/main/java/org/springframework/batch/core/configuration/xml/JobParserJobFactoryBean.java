/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This {@link FactoryBean} is used by the batch namespace parser to create
 * {@link FlowJob} objects. It stores all of the properties that are
 * configurable on the &lt;job/&gt;.
 * 
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0.1
 */
class JobParserJobFactoryBean implements SmartFactoryBean {

	private String name;

	private Boolean restartable;

	private JobRepository jobRepository;

	private JobParametersValidator jobParametersValidator;

	private JobExecutionListener[] jobExecutionListeners;

	private JobParametersIncrementer jobParametersIncrementer;

	private Flow flow;

	public JobParserJobFactoryBean(String name) {
		this.name = name;
	}

	public final Object getObject() throws Exception {
		Assert.isTrue(StringUtils.hasText(name), "The job must have an id.");
		FlowJob flowJob = new FlowJob(name);

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

	public void setJobExecutionListeners(JobExecutionListener[] jobExecutionListeners) {
		this.jobExecutionListeners = jobExecutionListeners;
	}

	public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
		this.jobParametersIncrementer = jobParametersIncrementer;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public Class<FlowJob> getObjectType() {
		return FlowJob.class;
	}

	public boolean isSingleton() {
		return true;
	}
	
	public boolean isEagerInit() {
		return true;
	}
	
	public boolean isPrototype() {
		return false;
	}

}
