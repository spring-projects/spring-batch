/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.support.ReflectionUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base class and utility for other step builders providing access to common properties like job repository and
 * transaction manager.
 * 
 * @author Dave Syer
 * @author Michael Minella
 *
 * @since 2.2
 */
public abstract class StepBuilderHelper<B extends StepBuilderHelper<B>> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final CommonStepProperties properties;

	public StepBuilderHelper(String name) {
		this.properties = new CommonStepProperties();
		properties.name = name;
	}

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	protected StepBuilderHelper(StepBuilderHelper<?> parent) {
		this.properties = new CommonStepProperties(parent.properties);
	}

	public B repository(JobRepository jobRepository) {
		properties.jobRepository = jobRepository;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	public B transactionManager(PlatformTransactionManager transactionManager) {
		properties.transactionManager = transactionManager;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	public B startLimit(int startLimit) {
		properties.startLimit = startLimit;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Registers objects using the annotation based listener configuration.
	 *
	 * @param listener the object that has a method configured with listener annotation
	 * @return this for fluent chaining
	 */
	public B listener(Object listener) {
		Set<Method> stepExecutionListenerMethods = new HashSet<>();
		stepExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeStep.class));
		stepExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterStep.class));

		if(stepExecutionListenerMethods.size() > 0) {
			StepListenerFactoryBean factory = new StepListenerFactoryBean();
			factory.setDelegate(listener);
			properties.addStepExecutionListener((StepExecutionListener) factory.getObject());
		}

		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	public B listener(StepExecutionListener listener) {
		properties.addStepExecutionListener(listener);
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	public B allowStartIfComplete(boolean allowStartIfComplete) {
		properties.allowStartIfComplete = allowStartIfComplete;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
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

		private List<StepExecutionListener> stepExecutionListeners = new ArrayList<>();

		private int startLimit = Integer.MAX_VALUE;

		private Boolean allowStartIfComplete;

		private JobRepository jobRepository;

		private PlatformTransactionManager transactionManager;

		public CommonStepProperties() {
		}

		public CommonStepProperties(CommonStepProperties properties) {
			this.name = properties.name;
			this.startLimit = properties.startLimit;
			this.allowStartIfComplete = properties.allowStartIfComplete;
			this.jobRepository = properties.jobRepository;
			this.transactionManager = properties.transactionManager;
			this.stepExecutionListeners = new ArrayList<>(properties.stepExecutionListeners);
		}

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
