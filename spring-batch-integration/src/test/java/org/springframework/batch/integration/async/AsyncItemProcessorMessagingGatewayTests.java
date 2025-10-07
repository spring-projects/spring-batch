/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.integration.async;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@TestExecutionListeners(listeners = StepScopeTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
// FIXME regression in late binding of job parameters in XML config
// (#{jobParameters['factor']})?
@Disabled
class AsyncItemProcessorMessagingGatewayTests {

	private AsyncItemProcessor<String, String> processor;

	@Autowired
	private ItemProcessor<String, String> delegate;

	StepExecution getStepExecution() {
		return MetaDataInstanceFactory
			.createStepExecution(new JobParametersBuilder().addLong("factor", 2L).toJobParameters());
	}

	@Test
	void testMultiExecution() throws Exception {
		processor = new AsyncItemProcessor<>(delegate);
		processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
		List<Future<String>> list = new ArrayList<>();
		for (int count = 0; count < 10; count++) {
			list.add(processor.process("foo" + count));
		}
		for (Future<String> future : list) {
			String value = future.get();
			/*
			 * This delegate is a Spring Integration MessagingGateway. It can easily
			 * return null because of a timeout, but that will be treated by Batch as a
			 * filtered item, whereas it is really more like a skip. So we have to throw
			 * an exception in the processor if an unexpected null value comes back.
			 */
			assertNotNull(value);
			assertTrue(value.matches("foo.*foo.*"));
		}
	}

	@MessageEndpoint
	static class Doubler {

		private int factor = 1;

		public void setFactor(int factor) {
			this.factor = factor;
		}

		@ServiceActivator
		public String cat(String value) {
			for (int i = 1; i < factor; i++) {
				value += value;
			}
			return value;
		}

	}

}
