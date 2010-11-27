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
package org.springframework.batch.integration.async;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PollingAsyncItemProcessorMessagingGatewayTests {

	private AsyncItemProcessor<String, String> processor = new AsyncItemProcessor<String, String>();

	private StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParametersBuilder().addLong("factor", 2L).toJobParameters());;

	@Rule
	public MethodRule rule = new MethodRule() {
		public Statement apply(final Statement base, FrameworkMethod method, Object target) {
			return new Statement() {
				public void evaluate() throws Throwable {
					StepScopeTestUtils.doInStepScope(stepExecution, new Callable<Void>() {
						public Void call() throws Exception {
							try {
								base.evaluate();
							}
							catch (Exception e) {
								throw e;
							}
							catch (Throwable e) {
								throw new Error(e);
							}
							return null;
						}
					});
				};
			};
		}
	};

	@Autowired
	private ItemProcessor<String, String> delegate;

	@Test
	public void testMultiExecution() throws Exception {
		processor.setDelegate(delegate);
		processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
		List<Future<String>> list = new ArrayList<Future<String>>();
		for (int count = 0; count < 10; count++) {
			list.add(processor.process("foo" + count));
		}
		for (Future<String> future : list) {
			String value = future.get();
			/**
			 * This delegate is a Spring Integration MessagingGateway. It can
			 * easily return null because of a timeout, but that will be treated
			 * by Batch as a filtered item, whereas it is really more like a
			 * skip. So we have to throw an exception in the processor if an
			 * unexpected null value comes back.
			 */
			assertNotNull(value);
			assertTrue(value.matches("foo.*foo.*"));
		}
	}

	@MessageEndpoint
	public static class Doubler {

		@ServiceActivator
		public String cat(String value, @Header(value="stepExecution.jobExecution.jobInstance.jobParameters.getLong('factor')", required=false) Integer input) {
			long factor = input==null ? 1 : input;
			for (int i=1; i<factor; i++) {
				value += value;
			}
			return value;
		}

	}

}
