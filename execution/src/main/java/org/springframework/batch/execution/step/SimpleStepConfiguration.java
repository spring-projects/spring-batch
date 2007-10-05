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

package org.springframework.batch.execution.step;

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.tasklet.Tasklet;

/**
 * Simple {@link StepConfiguration} good enough for most purposes and easy to
 * configure simple properties, principally the commit interval.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class SimpleStepConfiguration extends AbstractStepConfiguration {

	// default commit interval is one
	private int commitInterval = 1;

	public SimpleStepConfiguration() {
		super();
	}

	public SimpleStepConfiguration(String name) {
		super(name);
	}

	public SimpleStepConfiguration(Tasklet module) {
		this();
		setTasklet(module);
	}

	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	public int getCommitInterval() {
		return commitInterval;
	}

}
