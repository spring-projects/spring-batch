/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public abstract class StepBuilderHelper<B extends StepBuilderHelper<B>> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CommonStepProperties properties;

	public StepBuilderHelper(String name) {
		this.properties = new CommonStepProperties();
		properties.name = name;
	}

	protected StepBuilderHelper(StepBuilderHelper<?> parent) {
		this.properties = parent.properties;
	}

	public StepBuilderHelper<B> repository(JobRepository jobRepository) {
		properties.jobRepository = jobRepository;
		return this;
	}

	public StepBuilderHelper<B> transactionManager(PlatformTransactionManager transactionManager) {
		properties.transactionManager = transactionManager;
		return this;
	}

	public StepBuilderHelper<B> startLimit(int startLimit) {
		properties.startLimit = startLimit;
		return this;
	}

	public StepBuilderHelper<B> listener(StepExecutionListener listener) {
		properties.addStepExecutionListener(listener);
		return this;
	}

	public StepBuilderHelper<B> allowStartIfComplete(boolean allowStartIfComplete) {
		properties.allowStartIfComplete = allowStartIfComplete;
		return this;
	}

	protected String getName() {
		return properties.name;
	}

	protected JobRepository getJobRepository() {
		return properties.jobRepository;
	}

	protected PlatformTransactionManager getTransactionManager() {
		return properties.transactionManager;
	}

	protected boolean isAllowStartIfComplete() {
		return properties.allowStartIfComplete != null ? properties.allowStartIfComplete : false;
	}

	protected void enhance(Step target) {

		if (target instanceof AbstractStep) {

			AbstractStep step = (AbstractStep) target;
			step.setJobRepository(properties.getJobRepository());

			Boolean allowStartIfComplete = properties.allowStartIfComplete;
			if (allowStartIfComplete != null) {
				step.setAllowStartIfComplete(allowStartIfComplete);
			}

			step.setStartLimit(properties.startLimit);

			List<StepExecutionListener> listeners = properties.stepExecutionListeners;
			if (!listeners.isEmpty()) {
				step.setStepExecutionListeners(listeners.toArray(new StepExecutionListener[0]));
			}

		}

		if (target instanceof TaskletStep) {
			TaskletStep step = (TaskletStep) target;
			step.setTransactionManager(properties.transactionManager);
		}

	}

	public static class CommonStepProperties {

		private List<StepExecutionListener> stepExecutionListeners = new ArrayList<StepExecutionListener>();

		private int startLimit = Integer.MAX_VALUE;

		private Boolean allowStartIfComplete;

		private JobRepository jobRepository;

		private PlatformTransactionManager transactionManager;

		public JobRepository getJobRepository() {
			return jobRepository;
		}

		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

		public PlatformTransactionManager getTransactionManager() {
			return transactionManager;
		}

		public void setTransactionManager(PlatformTransactionManager transactionManager) {
			this.transactionManager = transactionManager;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<StepExecutionListener> getStepExecutionListeners() {
			return stepExecutionListeners;
		}

		public void addStepExecutionListeners(List<StepExecutionListener> stepExecutionListeners) {
			this.stepExecutionListeners.addAll(stepExecutionListeners);
		}

		public void addStepExecutionListener(StepExecutionListener stepExecutionListener) {
			this.stepExecutionListeners.add(stepExecutionListener);
		}

		public Integer getStartLimit() {
			return startLimit;
		}

		public void setStartLimit(Integer startLimit) {
			this.startLimit = startLimit;
		}

		public Boolean getAllowStartIfComplete() {
			return allowStartIfComplete;
		}

		public void setAllowStartIfComplete(Boolean allowStartIfComplete) {
			this.allowStartIfComplete = allowStartIfComplete;
		}

		private String name;

	}

}
