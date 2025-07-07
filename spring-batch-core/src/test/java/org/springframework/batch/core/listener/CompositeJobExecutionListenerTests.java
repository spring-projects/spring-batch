/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class CompositeJobExecutionListenerTests {

	private final CompositeJobExecutionListener listener = new CompositeJobExecutionListener();

	private final List<String> list = new ArrayList<>();

	@Test
	void testSetListeners() {
		listener.setListeners(Arrays.asList(new JobExecutionListener() {
			@Override
			public void afterJob(JobExecution jobExecution) {
				list.add("fail");
			}
		}, new JobExecutionListener() {
			@Override
			public void afterJob(JobExecution jobExecution) {
				list.add("continue");
			}
		}));
		listener.afterJob(null);
		assertEquals(2, list.size());
	}

	@Test
	void testSetListener() {
		listener.register(new JobExecutionListener() {
			@Override
			public void afterJob(JobExecution jobExecution) {
				list.add("fail");
			}
		});
		listener.afterJob(null);
		assertEquals(1, list.size());
	}

	@Test
	void testOpen() {
		listener.register(new JobExecutionListener() {
			@Override
			public void beforeJob(JobExecution stepExecution) {
				list.add("foo");
			}
		});
		listener.beforeJob(new JobExecution(new JobInstance(11L, "testOpenJob"), null));
		assertEquals(1, list.size());
	}

}
