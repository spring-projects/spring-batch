/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.step.listener;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

/**
 * <p>
 * {@link StepListener} for testing. Sets or appends the value of the
 * testProperty field to the {@link JobContext} exit status on afterStep.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class ExitStatusSettingStepListener implements StepListener {
	@Inject
	@BatchProperty
	private String testProperty;

	@Inject
	private JobContext jobContext;

	@Override
	public void beforeStep() throws Exception {

	}

	@Override
	public void afterStep() throws Exception {
		String exitStatus = jobContext.getExitStatus();

		if("".equals(exitStatus) || exitStatus == null) {
			jobContext.setExitStatus(testProperty);
		} else {
			jobContext.setExitStatus(exitStatus + testProperty);
		}
	}
}
