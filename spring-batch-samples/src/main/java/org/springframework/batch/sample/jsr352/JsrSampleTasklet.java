/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.batch.sample.jsr352;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

import javax.batch.api.BatchProperty;
import javax.inject.Inject;

/**
 * <p>
 * Sample {@link org.springframework.batch.core.step.tasklet.Tasklet} implementation.
 * </p>
 *
 * @since 3.0
 * @author Chris Schaefer
 */
public class JsrSampleTasklet implements Tasklet {
	private static final Log LOG = LogFactory.getLog(JsrSampleTasklet.class);

	@Inject
	@BatchProperty
	private String remoteServiceURL;

	@Nullable
	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		LOG.info("Calling remote service at: " + remoteServiceURL);

		Thread.sleep(2000);

		LOG.info("Remote service call complete");

		return RepeatStatus.FINISHED;
	}
}
