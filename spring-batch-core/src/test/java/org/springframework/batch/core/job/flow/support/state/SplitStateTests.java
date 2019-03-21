/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.job.flow.support.state;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.support.JobFlowExecutorSupport;
import org.springframework.core.task.SimpleAsyncTaskExecutor;


/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
public class SplitStateTests {

	private JobFlowExecutorSupport executor = new JobFlowExecutorSupport();

	@Test
	public void testBasicHandling() throws Exception {

		Collection<Flow> flows  = new ArrayList<>();
		Flow flow1 = mock(Flow.class);
		Flow flow2 = mock(Flow.class);
		flows.add(flow1);
		flows.add(flow2);

		SplitState state = new SplitState(flows, "foo");

		when(flow1.start(executor)).thenReturn(new FlowExecution("step1", FlowExecutionStatus.COMPLETED));
		when(flow2.start(executor)).thenReturn(new FlowExecution("step1", FlowExecutionStatus.COMPLETED));

		FlowExecutionStatus result = state.handle(executor);
		assertEquals(FlowExecutionStatus.COMPLETED, result);

	}

	@Test
	public void testConcurrentHandling() throws Exception {

		Flow flow1 = mock(Flow.class);
		Flow flow2 = mock(Flow.class);

		SplitState state = new SplitState(Arrays.asList(flow1, flow2), "foo");
		state.setTaskExecutor(new SimpleAsyncTaskExecutor());

		when(flow1.start(executor)).thenReturn(new FlowExecution("step1", FlowExecutionStatus.COMPLETED));
		when(flow2.start(executor)).thenReturn(new FlowExecution("step1", FlowExecutionStatus.COMPLETED));
		FlowExecutionStatus result = state.handle(executor);
		assertEquals(FlowExecutionStatus.COMPLETED, result);

	}

}
