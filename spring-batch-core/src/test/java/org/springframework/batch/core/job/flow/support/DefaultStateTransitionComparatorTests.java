/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.batch.core.job.flow.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.StateSupport;

class DefaultStateTransitionComparatorTests {

	private final State state = new StateSupport("state1");

	private final Comparator<StateTransition> comparator = new DefaultStateTransitionComparator();

	@Test
	void testSimpleOrderingEqual() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		assertEquals(0, comparator.compare(transition, transition));
	}

	@Test
	void testSimpleOrderingMoreGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, comparator.compare(transition, other));
		assertEquals(-1, comparator.compare(other, transition));
	}

	@Test
	void testSimpleOrderingMostGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, comparator.compare(transition, other));
		assertEquals(-1, comparator.compare(other, transition));
	}

	@Test
	void testSubstringAndWildcard() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, comparator.compare(transition, other));
		assertEquals(-1, comparator.compare(other, transition));
	}

	@Test
	void testSimpleOrderingMostToNextGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "C?", "start");
		assertEquals(1, comparator.compare(transition, other));
		assertEquals(-1, comparator.compare(other, transition));
	}

	@Test
	void testSimpleOrderingAdjacent() {
		StateTransition transition = StateTransition.createStateTransition(state, "CON*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CON?", "start");
		assertEquals(1, comparator.compare(transition, other));
		assertEquals(-1, comparator.compare(other, transition));
	}

}
