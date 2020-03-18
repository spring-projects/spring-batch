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
package org.springframework.batch.sample.loop;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.sample.domain.trade.internal.GeneratingTradeItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This listener resets the count of its GeneratingTradeItemReader after the
 * step.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class GeneratingTradeResettingListener extends StepExecutionListenerSupport implements InitializingBean {

	private GeneratingTradeItemReader reader;

	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		this.reader.resetCounter();
		return null;
	}

	public void setReader(GeneratingTradeItemReader reader) {
		this.reader = reader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.reader, "The 'reader' must be set.");
	}
}
