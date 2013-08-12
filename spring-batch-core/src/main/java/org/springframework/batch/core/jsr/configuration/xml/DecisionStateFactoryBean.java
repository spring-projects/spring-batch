/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.Decider;

import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.support.state.DecisionState;
import org.springframework.batch.core.jsr.DecisionAdapter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} for creating a {@link DecisionState}.  If the underlying
 * decider is a {@link Decider}, it will be wrapped in a {@link DecisionAdapter}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class DecisionStateFactoryBean implements FactoryBean<State>, InitializingBean {

	private Decider jsrDecider;
	private JobExecutionDecider decider;
	private String name;

	/**
	 * @param decider either a {@link Decider} or a {@link JobExecutionDecider}
	 * @throws IllegalArgumentException if the type passed in is not a valid type
	 */
	public void setDecider(Object decider) {
		if(decider instanceof JobExecutionDecider) {
			this.decider = (JobExecutionDecider) decider;
		} else if(decider instanceof Decider) {
			this.jsrDecider = (Decider) decider;
		} else {
			throw new IllegalArgumentException("Invalid type for a decider");
		}
	}

	/**
	 * The name of the state
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public State getObject() throws Exception {
		JobExecutionDecider wrappedDecider = null;

		if(decider != null) {
			wrappedDecider = decider;
		} else if(jsrDecider != null) {
			wrappedDecider = new DecisionAdapter(jsrDecider);
		}
		State state = new DecisionState(wrappedDecider, name);

		return state;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return JobExecutionDecider.class;
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
		Assert.isTrue(!(decider == null && jsrDecider == null), "A decider implementation is required");
		Assert.notNull(name, "A name is required for a decision state");
	}
}
