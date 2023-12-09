/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.integration;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class StepSupport implements Step {

	private final String name;

	private int startLimit = 1;

	/**
	 * @param name the step name
	 */
	public StepSupport(String name) {
		super();
		this.name = name;
	}

	@Override
	public void execute(StepExecution stepExecution) throws JobInterruptedException {
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getStartLimit() {
		return startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

}
