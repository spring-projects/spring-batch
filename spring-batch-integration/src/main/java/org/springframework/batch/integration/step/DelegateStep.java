/*
 * Copyright 2006-2010 the original author or authors.
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

package org.springframework.batch.integration.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

/**
 * Provides a wrapper for an existing {@link Step}, delegating execution to it,
 * but serving all other operations locally.
 * 
 * @author Dave Syer
 * 
 */
public class DelegateStep extends AbstractStep {

	private Step delegate;

	/**
	 * @param delegate the delegate to set
	 */
	public void setDelegate(Step delegate) {
		this.delegate = delegate;
	}
	
	/**
	 * Check mandatory properties (delegate).
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(delegate!=null, "A delegate Step must be provided");
		super.afterPropertiesSet();
	}

	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		delegate.execute(stepExecution);
	}

}
