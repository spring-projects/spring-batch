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

	/**
	 * {@link Log} to be used by the class.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The set of properties to be used when creating new {@link Step}s.
	 */
	protected final CommonStepProperties properties;

	/**
	 * Constructor for the {@link StepBuilderHelper}.
 	 * @param name to be by the {@link StepBuilderHelper} when creating new {@link Step}s.
	 */
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

	/**
	 * Set the {@link JobRepository} for the builder helper.
	 * @param jobRepository the {@link JobRepository} to use.
	 * @return this for fluent chaining.
	 */
	public B repository(JobRepository jobRepository) {
		properties.jobRepository = jobRepository;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Set the {@link PlatformTransactionManager} for the builder helper.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @return this for fluent chaining.
	 */
	public B transactionManager(PlatformTransactionManager transactionManager) {
		properties.transactionManager = transactionManager;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Set the startLimit for the builder helper.
	 * @param startLimit the start limit to use.
	 * @return this for fluent chaining.
	 */
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
	 * @return this for fluent chaining.
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

	/**
	 * Set the {@link StepExecutionListener} for the builder helper.
	 * @param listener the {@link StepExecutionListener} to use.
	 * @return this for fluent chaining.
	 */
	public B listener(StepExecutionListener listener) {
		properties.addStepExecutionListener(listener);
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Sets the allowStartIfComplete state.
	 * @param allowStartIfComplete value to set the allowStartIfComplete.
	 * @return this for fluent chaining.
	 */
	public B allowStartIfComplete(boolean allowStartIfComplete) {
		properties.allowStartIfComplete = allowStartIfComplete;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * @return nameused by the builder helper.
	 */
	protected String getName() {
		return properties.name;
	}

	/**
	 * @return {@link JobRepository} used by the builder helper.
	 */
	protected JobRepository getJobRepository() {
		return properties.jobRepository;
	}

	/**
 	 * @return {@link PlatformTransactionManager} used by the builder helper.
	 */
	protected PlatformTransactionManager getTransactionManager() {
		return properties.transactionManager;
	}

	/**
	 * @return true if start is allowed if step is complete.
	 */
	protected boolean isAllowStartIfComplete() {
		return properties.allowStartIfComplete != null ? properties.allowStartIfComplete : false;
	}

	/**
	 * Set {@link CommonStepProperties} for the target {@link Step}.
	 * @param target {@link Step} that will be enhanced.
	 */
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

	/**
	 * Common properties used for creating {@link Step}s.
	 */
	public static class CommonStepProperties {

		private List<StepExecutionListener> stepExecutionListeners = new ArrayList<>();

		private int startLimit = Integer.MAX_VALUE;

		private Boolean allowStartIfComplete;

		private JobRepository jobRepository;

		private PlatformTransactionManager transactionManager;

		/**
		 * Default constructor.
		 */
		public CommonStepProperties() {
		}

		/**
		 * Constructor for {@link CommonStepProperties}
		 * @param properties {@link CommonStepProperties} used to initialize the instance.
		 */
		public CommonStepProperties(CommonStepProperties properties) {
			this.name = properties.name;
			this.startLimit = properties.startLimit;
			this.allowStartIfComplete = properties.allowStartIfComplete;
			this.jobRepository = properties.jobRepository;
			this.transactionManager = properties.transactionManager;
			this.stepExecutionListeners = new ArrayList<>(properties.stepExecutionListeners);
		}

		/**
		 * @return the {@link JobRepository} used by the builder helper.
		 */
		public JobRepository getJobRepository() {
			return jobRepository;
		}

		/**
		 * Set the {@link JobRepository} to be used by the builder helper.
		 * @param jobRepository the {@link JobRepository} to be used by the builder helper.
		 */
		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

		/**
		 * @return the {@link PlatformTransactionManager} used by the builder helper.
		 */
		public PlatformTransactionManager getTransactionManager() {
			return transactionManager;
		}

		/**
		 * Set the {@link PlatformTransactionManager} to be used by the builder helper.
		 * @param transactionManager the {@link PlatformTransactionManager} to be used by the builder helper.
		 */
		public void setTransactionManager(PlatformTransactionManager transactionManager) {
			this.transactionManager = transactionManager;
		}

		/**
		 * @return the name being used by the builder helper.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Set the name to be used by the builder helper.
 		 * @param name the name to be used by the builder helper.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return {@link List} of {@link StepExecutionListener}s used by builder helper.
		 */
		public List<StepExecutionListener> getStepExecutionListeners() {
			return stepExecutionListeners;
		}

		/**
		 * Adds a {@link List} of {@link StepExecutionListener}s to the list of listeners used by the builder helper.
		 * @param stepExecutionListeners {@link List} of {@link StepExecutionListener}s to be added to the listeners used by the builder helper.
		 */
		public void addStepExecutionListeners(List<StepExecutionListener> stepExecutionListeners) {
			this.stepExecutionListeners.addAll(stepExecutionListeners);
		}

		/**
		 * Adds a {@link StepExecutionListener} to the list of listeners used by the builder helper.
		 * @param stepExecutionListener {@link StepExecutionListener} to be added to the listeners used by the builder helper.
		 */
		public void addStepExecutionListener(StepExecutionListener stepExecutionListener) {
			this.stepExecutionListeners.add(stepExecutionListener);
		}

		/**
		 * @return the startLimit used by the builder helper.
		 */
		public Integer getStartLimit() {
			return startLimit;
		}

		/**
		 * Set the start limit used by the step builder helper.
		 * @param startLimit the startLimit to be use by the builder helper.
		 */
		public void setStartLimit(Integer startLimit) {
			this.startLimit = startLimit;
		}

		/**
		 * @return the current value of allowStartIfComplete.
		 */
		public Boolean getAllowStartIfComplete() {
			return allowStartIfComplete;
		}

		/**
		 * Set the value for allowStartIfComplete for the builder helper.
 		 * @param allowStartIfComplete the allowStartIfComplete to be used by the builder helper.
		 */
		public void setAllowStartIfComplete(Boolean allowStartIfComplete) {
			this.allowStartIfComplete = allowStartIfComplete;
		}

		private String name;

	}

}
