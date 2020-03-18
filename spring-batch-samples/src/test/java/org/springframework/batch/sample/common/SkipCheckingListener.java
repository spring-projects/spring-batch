/*
 * Copyright 2008-2009 the original author or authors.
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
package org.springframework.batch.sample.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class SkipCheckingListener {

	private static final Log logger = LogFactory.getLog(SkipCheckingListener.class);
	private static int processSkips;

	@AfterStep
	public ExitStatus checkForSkips(StepExecution stepExecution) {
		if (!stepExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new ExitStatus("COMPLETED WITH SKIPS");
		}
		else {
			return null;
		}
	}
	
	/**
	 * Convenience method for testing
	 * @return the processSkips
	 */
	public static int getProcessSkips() {
		return processSkips;
	}

	/**
	 * Convenience method for testing
	 */
	public static void resetProcessSkips() {
		processSkips = 0;
	}

	@OnSkipInWrite
	public void skipWrite(Trade trade, Throwable t) {
		logger.debug("Skipped writing " + trade);
	}

	@OnSkipInProcess
	public void skipProcess(Trade trade, Throwable t) {
		logger.debug("Skipped processing " + trade);
		processSkips++;
	}

	@BeforeStep
	public void saveStepName(StepExecution stepExecution) {
		stepExecution.getExecutionContext().put("stepName", stepExecution.getStepName());
	}
}
