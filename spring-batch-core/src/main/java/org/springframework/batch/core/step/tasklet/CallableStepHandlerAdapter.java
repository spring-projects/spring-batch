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
package org.springframework.batch.core.step.tasklet;

import java.util.concurrent.Callable;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.AttributeAccessor;
import org.springframework.util.Assert;

/**
 * Adapts a {@link Callable}&lt;{@link ExitStatus}&gt; to the
 * {@link StepHandler} interface.
 * 
 * @author Dave Syer
 * 
 */
public class CallableStepHandlerAdapter implements StepHandler, InitializingBean {

	private Callable<ExitStatus> callable;
	
	/**
	 * Public setter for the {@link Callable}.
	 * @param callable the {@link Callable} to set
	 */
	public void setCallable(Callable<ExitStatus> callable) {
		this.callable = callable;
	}
	
	/**
	 * Assert that the callable is set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(callable);
	}

	/**
	 * Execute the provided Callable and return its {@link ExitStatus}.  Ignores
	 * the {@link StepContribution} and the attributes.
	 * @see StepHandler#handle(StepContribution, AttributeAccessor)
	 */
	public ExitStatus handle(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		return callable.call();
	}

}
