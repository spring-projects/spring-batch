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
package org.springframework.batch.integration.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.Test;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;

public class AsyncItemProcessorTests {

	private AsyncItemProcessor<String, String> processor = new AsyncItemProcessor<>();

	private ItemProcessor<String, String> delegate = new ItemProcessor<String, String>() {
		@Nullable
		public String process(String item) throws Exception {
			return item + item;
		};
	};

	@Test(expected = IllegalArgumentException.class)
	public void testNoDelegate() throws Exception {
		processor.afterPropertiesSet();
	}

	@Test
	public void testExecution() throws Exception {
		processor.setDelegate(delegate);
		Future<String> result = processor.process("foo");
		assertEquals("foofoo", result.get());
	}

	@Test
	public void testExecutionInStepScope() throws Exception {
		delegate = new ItemProcessor<String, String>() {
			@Nullable
			public String process(String item) throws Exception {
				StepContext context = StepSynchronizationManager.getContext();
				assertTrue(context != null && context.getStepExecution() != null);
				return item + item;
			};
		};
		processor.setDelegate(delegate);
		Future<String> result = StepScopeTestUtils.doInStepScope(MetaDataInstanceFactory.createStepExecution(), new Callable<Future<String>>() {
			public Future<String> call() throws Exception {
				return processor.process("foo");
			}
		});
		assertEquals("foofoo", result.get());
	}

	@Test
	public void testMultiExecution() throws Exception {
		processor.setDelegate(delegate);
		processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
		List<Future<String>> list = new ArrayList<>();
		for (int count = 0; count < 10; count++) {
			list.add(processor.process("foo" + count));
		}
		for (Future<String> future : list) {
			assertTrue(future.get().matches("foo.*foo.*"));
		}
	}

}
