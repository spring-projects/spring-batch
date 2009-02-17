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
package org.springframework.batch.core.job.flow.support.state;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.support.JobFlowExecutorSupport;

/**
 * @author Dave Syer
 *
 */
public class EndStateTests {

	private JobExecution jobExecution;
	
	@Before
	public void setUp() {
		jobExecution = new JobExecution(0L);
	}

	/**
	 * Test method for {@link EndState#handle(FlowExecutor)}.
	 * @throws Exception 
	 */
	@Test
	public void testHandleRestartSunnyDay() throws Exception {

		BatchStatus status = jobExecution.getStatus();
	
		EndState state = new EndState(FlowExecutionStatus.UNKNOWN, "end");
		state.handle(new JobFlowExecutorSupport() {
			@Override
			public JobExecution getJobExecution() {
				return jobExecution;
			}
		});
		
		assertEquals(status, jobExecution.getStatus());

	}

	/**
	 * Test method for {@link EndState#handle(FlowExecutor)}.
	 * @throws Exception 
	 */
	@Test
	public void testHandleOngoingSunnyDay() throws Exception {

		jobExecution.createStepExecution("foo");
	
		EndState state = new EndState(FlowExecutionStatus.UNKNOWN, "end");
		FlowExecutionStatus status = state.handle(new JobFlowExecutorSupport() {
			@Override
			public JobExecution getJobExecution() {
				return jobExecution;
			}
		});
		
		assertEquals(FlowExecutionStatus.UNKNOWN, status);

	}

}
