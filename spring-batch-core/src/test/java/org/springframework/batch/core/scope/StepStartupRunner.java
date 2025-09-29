/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.scope;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.InitializingBean;

public class StepStartupRunner implements InitializingBean {

	private Step step;

	public void setStep(Step step) {
		this.step = step;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		StepExecution stepExecution = new StepExecution(0L, "step",
				new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters()));
		step.execute(stepExecution);
		// expect no errors
	}

}
