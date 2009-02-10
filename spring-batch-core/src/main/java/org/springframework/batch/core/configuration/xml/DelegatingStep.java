/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * Class used for delegating from a <step> definition to an individual bean
 * defining the actual step. This is needed to maintain the id from the <step>
 * element for any flow references.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class DelegatingStep implements Step {
	
	String name;
	Step delegate;
	
	/**
	 * Constructor taking the step name and the delegate as parameters.
	 * 
	 * @param name the name to be used for the step
	 * @param delegate the step definition to delegate to
	 */
	public DelegatingStep(String name, Step delegate) {
		this.name = name;
		this.delegate = delegate;
	}

	public void execute(StepExecution stepExecution)
			throws JobInterruptedException {
		delegate.execute(stepExecution);
	}

	public String getName() {
		return name;
	}

	public int getStartLimit() {
		return delegate.getStartLimit();
	}

	public boolean isAllowStartIfComplete() {
		return delegate.isAllowStartIfComplete();
	}

}
