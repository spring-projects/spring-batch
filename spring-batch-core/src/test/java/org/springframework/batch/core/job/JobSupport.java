/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.core.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Batch domain object representing a job. Job is an explicit abstraction representing the
 * configuration of a job specified by a developer. It should be noted that restart policy
 * is applied to the job as a whole and not to a step.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class JobSupport implements BeanNameAware, Job, StepLocator {

	private Map<String, Step> steps = new HashMap<>();

	private String name;

	private boolean restartable = false;

	private int startLimit = Integer.MAX_VALUE;

	private DefaultJobParametersValidator jobParametersValidator = new DefaultJobParametersValidator();

	/**
	 * Default constructor.
	 */
	public JobSupport() {
		super();
	}

	/**
	 * Convenience constructor to immediately add name (which is mandatory but not final).
	 * @param name
	 */
	public JobSupport(String name) {
		super();
		this.name = name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of the
	 * callbacks in a Spring container the name property will be set first if it is
	 * present. Care is needed with bean definition inheritance - if a parent bean has a
	 * name, then its children need an explicit name as well, otherwise they will not be
	 * unique.
	 *
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * Set the name property. Always overrides the default value if this object is a
	 * Spring bean.
	 *
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.IJob#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param jobParametersValidator the jobParametersValidator to set
	 */
	public void setJobParametersValidator(DefaultJobParametersValidator jobParametersValidator) {
		this.jobParametersValidator = jobParametersValidator;
	}

	public void setSteps(List<Step> steps) {
		this.steps.clear();
		for (Step step : steps) {
			this.steps.put(step.getName(), step);
		}
	}

	public void addStep(Step step) {
		this.steps.put(step.getName(), step);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.IJob#getStartLimit()
	 */
	public int getStartLimit() {
		return startLimit;
	}

	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.IJob#isRestartable()
	 */
	@Override
	public boolean isRestartable() {
		return restartable;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.Job#run(org.springframework.batch
	 * .core.domain.JobExecution)
	 */
	@Override
	public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
		throw new UnsupportedOperationException(
				"JobSupport does not provide an implementation of execute().  Use a smarter subclass.");
	}

	@Override
	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.Job#getJobParametersIncrementer()
	 */
	@Nullable
	@Override
	public JobParametersIncrementer getJobParametersIncrementer() {
		return null;
	}

	@Override
	public JobParametersValidator getJobParametersValidator() {
		return jobParametersValidator;
	}

	@Override
	public Collection<String> getStepNames() {
		return steps.keySet();
	}

	@Override
	public Step getStep(String stepName) throws NoSuchStepException {
		final Step step = steps.get(stepName);
		if (step == null) {
			throw new NoSuchStepException(
					"Step [" + stepName + "] does not exist for job with name [" + getName() + "]");
		}
		return step;
	}

}
