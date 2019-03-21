/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 */
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, JobScopeTestExecutionListener.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class JobScopeTestExecutionListenerIntegrationTests {

	@Autowired
	private ItemReader<String> reader;

	@Autowired
	private ItemStream stream;

	public JobExecution getJobExecution() {
		// Assert that dependencies are already injected...
		assertNotNull(reader);
		// Then create the execution for the job scope...
		JobExecution execution = MetaDataInstanceFactory.createJobExecution();
		execution.getExecutionContext().putString("input.file", "classpath:/org/springframework/batch/test/simple.txt");
		return execution;
	}

	@Test
	public void testJob() throws Exception {
		stream.open(new ExecutionContext());
		assertEquals("foo", reader.read());
	}

}
