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
package org.springframework.batch.core.step.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.infrastructure.support.ReflectionUtils;

/**
 * A base class and utility for other step builders providing access to common properties
 * like job repository and listeners.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Taeik Lim
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
@NullUnmarked // FIXME to remove once default constructors are removed
public abstract class StepBuilderHelper<B extends StepBuilderHelper<B>> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final CommonStepProperties properties;

	/**
	 * Create a new {@link StepBuilderHelper} with the given job repository.
	 * @param jobRepository the job repository
	 * @since 6.0
	 */
	public StepBuilderHelper(JobRepository jobRepository) {
		this.properties = new CommonStepProperties();
		properties.jobRepository = jobRepository;
	}

	/**
	 * Create a new {@link StepBuilderHelper}.
	 * @param name the step name
	 * @param jobRepository the job repository
	 * @since 5.1
	 */
	public StepBuilderHelper(String name, JobRepository jobRepository) {
		this.properties = new CommonStepProperties();
		properties.name = name;
		properties.jobRepository = jobRepository;
	}

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is
	 * copied, so it can be re-used.
	 * @param parent a parent helper containing common step properties
	 */
	protected StepBuilderHelper(StepBuilderHelper<?> parent) {
		this.properties = new CommonStepProperties(parent.properties);
	}

	public B observationRegistry(ObservationRegistry observationRegistry) {
		properties.observationRegistry = observationRegistry;
		return self();
	}

	public B startLimit(int startLimit) {
		properties.startLimit = startLimit;
		return self();
	}

	/**
	 * Registers objects using the annotation based listener configuration.
	 * @param listener the object that has a method configured with listener annotation
	 * @return this for fluent chaining
	 */
	public B listener(Object listener) {
		Set<Method> stepExecutionListenerMethods = new HashSet<>();
		stepExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeStep.class));
		stepExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterStep.class));

		if (stepExecutionListenerMethods.size() > 0) {
			StepListenerFactoryBean factory = new StepListenerFactoryBean();
			factory.setDelegate(listener);
			properties.addStepExecutionListener((StepExecutionListener) factory.getObject());
		}

		return self();
	}

	public B listener(StepExecutionListener listener) {
		properties.addStepExecutionListener(listener);
		return self();
	}

	public B allowStartIfComplete(boolean allowStartIfComplete) {
		properties.allowStartIfComplete = allowStartIfComplete;
		return self();
	}

	protected abstract B self();

	protected String getName() {
		return properties.name;
	}

	protected JobRepository getJobRepository() {
		return properties.jobRepository;
	}

	protected boolean isAllowStartIfComplete() {
		return properties.allowStartIfComplete != null ? properties.allowStartIfComplete : false;
	}

	protected void enhance(AbstractStep step) {
		step.setJobRepository(properties.getJobRepository());

		ObservationRegistry observationRegistry = properties.getObservationRegistry();
		if (observationRegistry != null) {
			step.setObservationRegistry(observationRegistry);
		}

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

	public static class CommonStepProperties {

		private String name;

		private List<StepExecutionListener> stepExecutionListeners = new ArrayList<>();

		private int startLimit = Integer.MAX_VALUE;

		private Boolean allowStartIfComplete;

		private JobRepository jobRepository;

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		public CommonStepProperties() {
		}

		public CommonStepProperties(CommonStepProperties properties) {
			this.name = properties.name;
			this.startLimit = properties.startLimit;
			this.allowStartIfComplete = properties.allowStartIfComplete;
			this.jobRepository = properties.jobRepository;
			this.observationRegistry = properties.observationRegistry;
			this.stepExecutionListeners = new ArrayList<>(properties.stepExecutionListeners);
		}

		public JobRepository getJobRepository() {
			return jobRepository;
		}

		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

		public ObservationRegistry getObservationRegistry() {
			return observationRegistry;
		}

		public void setObservationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
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

	}

}
