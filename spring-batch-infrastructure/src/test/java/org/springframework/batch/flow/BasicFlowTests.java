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
package org.springframework.batch.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.flow.FlowExecution;
import org.springframework.batch.flow.FlowExecutionException;
import org.springframework.batch.flow.SimpleFlow;
import org.springframework.batch.flow.StateTransition;

/**
 * @author Dave Syer
 * 
 */
public class BasicFlowTests {

	private SimpleFlow<Object> flow = new SimpleFlow<Object>("job");

	private Object executor = "data";

	@Test(expected = IllegalArgumentException.class)
	public void testEmptySteps() throws Exception {
		flow.setStateTransitions(Collections.<StateTransition<Object>> emptySet());
		flow.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoNextStepSpecified() throws Exception {
		flow.setStateTransitions(Collections.singleton(StateTransition.createStateTransition(new StateSupport<Object>(
				"step"), "foo")));
		flow.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoStartStep() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StateSupport<Object>("step"),
				FlowExecution.FAILED, "step"), StateTransition
				.createEndStateTransition(new StateSupport<Object>("step"))));
		flow.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoEndStep() throws Exception {
		flow.setStateTransitions(Collections.singleton(StateTransition.createStateTransition(new StateSupport<Object>(
				"step"), FlowExecution.FAILED, "step")));
		flow.setStartStateName("step");
		flow.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMultipleStartSteps() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createEndStateTransition(new StubState("step1")),
				StateTransition.createEndStateTransition(new StubState("step2"))));
		flow.afterPropertiesSet();
	}

	@Test
	public void testNoMatchForNextStep() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1"), "FOO", "step2"),
				StateTransition.createEndStateTransition(new StubState("step2"))));
		flow.afterPropertiesSet();
		try {
			flow.start(executor);
			fail("Expected JobExecutionException");
		}
		catch (FlowExecutionException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.toLowerCase().contains("next state not found"));
		}
	}

	@Test
	public void testOneStep() throws Exception {
		flow.setStateTransitions(Collections
				.singleton(StateTransition.createEndStateTransition(new StubState("step1"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step1", execution.getName());
	}

	@Test
	public void testOneStepWithListenerCallsClose() throws Exception {
		flow.setStateTransitions(Collections
				.singleton(StateTransition.createEndStateTransition(new StubState("step1"))));
		flow.afterPropertiesSet();
		final List<FlowExecution> list = new ArrayList<FlowExecution>();
		executor = new FlowExecutionListenerSupport() {
			@Override
			public void close(FlowExecution result) {
				list.add(result);
			}
		};
		FlowExecution execution = flow.start(executor);
		assertEquals(1, list.size());
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step1", execution.getName());
	}

	@Test
	public void testExplicitStartStep() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step"),
				FlowExecution.FAILED, "step"), StateTransition.createEndStateTransition(new StubState("step"))));
		flow.setStartStateName("step");
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step", execution.getName());
	}

	@Test
	public void testTwoSteps() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1"), "step2"),
				StateTransition.createEndStateTransition(new StubState("step2"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step2", execution.getName());
	}

	@Test
	public void testResume() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1"), "step2"),
				StateTransition.createEndStateTransition(new StubState("step2"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.resume("step2", executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step2", execution.getName());
	}

	@Test
	public void testFailedStep() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1") {
			@Override
			public String handle(Object executor) {
				return FlowExecution.FAILED;
			}
		}, "step2"), StateTransition.createEndStateTransition(new StubState("step2"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step2", execution.getName());
	}

	@Test
	public void testBranching() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1"), "step2"),
				StateTransition.createStateTransition(new StubState("step1"), FlowExecution.COMPLETED, "step3"),
				StateTransition.createEndStateTransition(new StubState("step2")), StateTransition
						.createEndStateTransition(new StubState("step3"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step3", execution.getName());
	}

	@Test
	public void testPause() throws Exception {
		flow.setStateTransitions(collect(StateTransition.createStateTransition(new StubState("step1"), "step2"),
				StateTransition.createStateTransition(new StubState("step2") {
					private boolean paused = false;

					@Override
					public String handle(Object executor) throws Exception {
						if (!paused) {
							paused = true;
							return FlowExecution.PAUSED;
						}
						paused = false;
						return FlowExecution.COMPLETED;
					}

				}, "step3"), StateTransition.createEndStateTransition(new StubState("step3"))));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecution.PAUSED, execution.getStatus());
		assertEquals("step2", execution.getName());
		execution = flow.resume(execution.getName(), executor);
		assertEquals(FlowExecution.COMPLETED, execution.getStatus());
		assertEquals("step3", execution.getName());
	}

	private Collection<StateTransition<Object>> collect(StateTransition<Object> s1, StateTransition<Object> s2) {
		Collection<StateTransition<Object>> list = new ArrayList<StateTransition<Object>>();
		list.add(s1);
		list.add(s2);
		return list;
	}

	private Collection<StateTransition<Object>> collect(StateTransition<Object> s1, StateTransition<Object> s2,
			StateTransition<Object> s3) {
		Collection<StateTransition<Object>> list = collect(s1, s2);
		list.add(s3);
		return list;
	}

	private Collection<StateTransition<Object>> collect(StateTransition<Object> s1, StateTransition<Object> s2,
			StateTransition<Object> s3, StateTransition<Object> s4) {
		Collection<StateTransition<Object>> list = collect(s1, s2, s3);
		list.add(s4);
		return list;
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class StubState extends StateSupport<Object> {

		/**
		 * @param string
		 */
		public StubState(String string) {
			super(string);
		}

	}

}
