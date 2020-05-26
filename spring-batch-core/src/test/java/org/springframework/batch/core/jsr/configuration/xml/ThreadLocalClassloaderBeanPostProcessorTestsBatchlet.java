/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.springframework.util.Assert;

public class ThreadLocalClassloaderBeanPostProcessorTestsBatchlet implements Batchlet {
	@Inject
	@BatchProperty
	public String jobParam1;

	@Inject
	public JobContext jobContext;

	@Inject
	public StepContext stepContext;

	@Override
	public String process() throws Exception {
		Assert.isTrue("someParameter".equals(jobParam1), jobParam1 + " does not equal someParameter");
		Assert.isTrue("threadLocalClassloaderBeanPostProcessorTestsJob".equals(jobContext.getJobName()),
				"jobName does not equal threadLocalClassloaderBeanPostProcessorTestsJob");
		Assert.isTrue("step1".equals(stepContext.getStepName()), "stepName does not equal step1");

		return null;
	}

	@Override
	public void stop() throws Exception {
	}
}
