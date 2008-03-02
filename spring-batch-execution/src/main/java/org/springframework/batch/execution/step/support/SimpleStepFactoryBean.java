/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * @author Dave Syer
 *
 */
public class SimpleStepFactoryBean extends AbstractStepFactoryBean {

	private int commitInterval = 0;

	/**
	 * Set the commit interval.
	 * 
	 * @param commitInterval
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {
		
		super.applyConfiguration(step);
		
		if (commitInterval > 0) {
			RepeatTemplate chunkOperations = new RepeatTemplate();
			chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(commitInterval));
			step.setChunkOperations(chunkOperations);
		}

	}
}
