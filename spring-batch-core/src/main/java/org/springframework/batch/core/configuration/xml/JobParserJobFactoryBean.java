/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.job.parameters.JobParametersIncrementer;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This {@link FactoryBean} is used by the batch namespace parser to create
 * {@link FlowJob} objects. It stores all of the properties that are configurable on the
 * &lt;job/&gt;.
 *
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0.1
 */
public class JobParserJobFactoryBean implements SmartFactoryBean<FlowJob> {

	private final String name;

	private Boolean restartable;

	private JobRepository jobRepository;

	private JobParametersValidator jobParametersValidator;

	private JobExecutionListener[] jobExecutionListeners;

	private JobParametersIncrementer jobParametersIncrementer;

	private Flow flow;

	/**
	 * Constructor for the factory bean that initializes the name.
	 * @param name The name to be used by the factory bean.
	 */
	public JobParserJobFactoryBean(String name) {
		this.name = name;
	}

	@Override
	public final FlowJob getObject() throws Exception {
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

	/**
	 * Set the restartable flag for the factory bean.
	 * @param restartable The restartable flag to be used by the factory bean.
	 */
	public void setRestartable(Boolean restartable) {
		this.restartable = restartable;
	}

	/**
	 * Set the {@link JobRepository} for the factory bean.
	 * @param jobRepository The {@link JobRepository} to be used by the factory bean.
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Set the {@link JobParametersValidator} for the factory bean.
	 * @param jobParametersValidator The {@link JobParametersValidator} to be used by the
	 * factory bean.
	 */
	public void setJobParametersValidator(JobParametersValidator jobParametersValidator) {
		this.jobParametersValidator = jobParametersValidator;
	}

	/**
	 * @return The {@link JobRepository} used by the factory bean.
	 */
	public JobRepository getJobRepository() {
		return this.jobRepository;
	}

	public void setJobExecutionListeners(JobExecutionListener[] jobExecutionListeners) {
		this.jobExecutionListeners = jobExecutionListeners;
	}

	/**
	 * Set the {@link JobParametersIncrementer} for the factory bean.
	 * @param jobParametersIncrementer The {@link JobParametersIncrementer} to be used by
	 * the factory bean.
	 */
	public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
		this.jobParametersIncrementer = jobParametersIncrementer;
	}

	/**
	 * Set the flow for the factory bean.
	 * @param flow The {@link Flow} to be used by the factory bean.
	 */
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

}
