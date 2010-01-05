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
package org.springframework.batch.integration;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 *
 */
public class StepSupport implements Step {

	private String name;
	private int startLimit = 1;

	/**
	 * @param name
	 */
	public StepSupport(String name) {
		super();
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.Step#execute(org.springframework.batch.core.StepExecution)
	 */
	public void execute(StepExecution stepExecution) throws JobInterruptedException {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.Step#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.Step#getStartLimit()
	 */
	public int getStartLimit() {
		return startLimit;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.Step#isAllowStartIfComplete()
	 */
	public boolean isAllowStartIfComplete() {
		return false;
	}

	/**
	 * Public setter for the startLimit.
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

}
