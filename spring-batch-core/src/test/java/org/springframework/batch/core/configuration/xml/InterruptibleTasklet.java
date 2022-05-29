/*
 * Copyright 2006-2019 the original author or authors.
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

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

/**
 * This tasklet will call
 * {@link NameStoringTasklet#execute(StepContribution, ChunkContext)} and then return
 * CONTINUABLE, so it needs to be interrupted for it to stop.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class InterruptibleTasklet extends NameStoringTasklet {

	private volatile boolean started = false;

	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		if (!started) {
			super.execute(contribution, chunkContext);
			started = true;
		}
		Thread.sleep(50L);
		return RepeatStatus.CONTINUABLE;
	}

}
