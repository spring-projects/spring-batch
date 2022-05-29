/*
 * Copyright 2006-2013 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.BeanNameAware;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class DummyStep implements Step, BeanNameAware {

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	@Override
	public void execute(StepExecution stepExecution) throws JobInterruptedException {
		System.out.println("EXECUTING " + getName());
	}

	@Override
	public int getStartLimit() {
		return 100;
	}

	@Override
	public boolean isAllowStartIfComplete() {
		return false;
	}

}
