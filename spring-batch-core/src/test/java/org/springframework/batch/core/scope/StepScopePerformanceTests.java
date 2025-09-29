/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StopWatch;

@SpringJUnitConfig
public class StepScopePerformanceTests implements ApplicationContextAware {

	private final Log logger = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;

	}

	@BeforeEach
	void start() throws Exception {
		int count = doTest("vanilla", "warmup");
		logger.info("Item count: " + count);
		StepSynchronizationManager.close();
		StepSynchronizationManager
			.register(new StepExecution("step", new JobExecution(0L, new JobInstance(1L, "job"), new JobParameters())));
	}

	@AfterEach
	void cleanup() {
		StepSynchronizationManager.close();
	}

	@Test
	void testVanilla() throws Exception {
		int count = doTest("vanilla", "vanilla");
		logger.info("Item count: " + count);
	}

	@Test
	void testProxied() throws Exception {
		int count = doTest("proxied", "proxied");
		logger.info("Item count: " + count);
	}

	private int doTest(String name, String test) throws Exception {
		@SuppressWarnings("unchecked")
		ItemStreamReader<String> reader = (ItemStreamReader<String>) applicationContext.getBean(name);
		reader.open(new ExecutionContext());
		StopWatch stopWatch = new StopWatch(test);
		stopWatch.start();
		int count = 0;
		while (reader.read() != null) {
			// do nothing
			count++;
		}
		stopWatch.stop();
		reader.close();
		logger.info(stopWatch.shortSummary());
		return count;
	}

}
