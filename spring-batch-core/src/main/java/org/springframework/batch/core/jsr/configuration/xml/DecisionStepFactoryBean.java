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

import javax.batch.api.Decider;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.jsr.step.DecisionStep;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} for creating a {@link DecisionStep}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class DecisionStepFactoryBean implements FactoryBean<Step>, InitializingBean {

	private Decider jsrDecider;
	private String name;
	private JobRepository jobRepository;

	/**
	 * @param jobRepository All steps need to be able to reference a {@link JobRepository}
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * @param decider a {@link Decider}
	 * @throws IllegalArgumentException if the type passed in is not a valid type
	 */
	public void setDecider(Decider decider) {
		this.jsrDecider = decider;
	}

	/**
	 * The name of the state
	 *
	 * @param name the name to be used by the DecisionStep.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public Step getObject() throws Exception {

		DecisionStep decisionStep = new DecisionStep(jsrDecider);
		decisionStep.setName(name);
		decisionStep.setJobRepository(jobRepository);
		decisionStep.setAllowStartIfComplete(true);

		return decisionStep;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return DecisionStep.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(jsrDecider != null, "A decider implementation is required");
		Assert.notNull(name, "A name is required for a decision state");
	}
}
