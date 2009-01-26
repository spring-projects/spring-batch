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
package org.springframework.batch.core.listener;

import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step;

/**
 * This class can be used to automatically promote items from the {@link Step}
 * {@link ExecutionContext} to the {@link Job} {@link ExecutionContext} at the
 * end of a step. A list of keys should be provided that correspond to the items
 * in the {@link Step} {@link ExecutionContext} that should be promoted.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class ExecutionContextPromotionListener extends StepExecutionListenerSupport implements InitializingBean {

	private List<String> keys = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.domain.StepListener#afterStep(StepExecution
	 *      stepExecution)
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		ExecutionContext stepContext = stepExecution.getExecutionContext();
		ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
		for (String key : keys) {
			jobContext.put(key, stepContext.get(key));
		}
		return null;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.keys, "The 'keys' property must be provided");
	}

	public void setKeys(List<String> keys) {
		this.keys = keys;
	}
}
