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
package org.springframework.batch.core.job.flow.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.StateSupport;

/**
 * @author Dave Syer
 * 
 */
public class StateTransitionTests {

	State state = new StateSupport("state1");
	
	@Test
	public void testIsEnd() {
		StateTransition transition = StateTransition.createEndStateTransition(state, "");
		assertTrue(transition.isEnd());
		assertNull(transition.getNext());
	}

	@Test
	public void testMatchesStar() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesNull() {
		StateTransition transition = StateTransition.createStateTransition(state, null, "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesEmpty() {
		StateTransition transition = StateTransition.createStateTransition(state, "", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesExact() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesWildcard() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesPlaceholder() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testSimpleOrderingEqual() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		assertEquals(0, transition.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMoreGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSubstringAndWildcard() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostToNextGeneral() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "C?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingAdjacent() {
		StateTransition transition = StateTransition.createStateTransition(state, "CON*", "start");
		StateTransition other = StateTransition.createStateTransition(state, "CON?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testToString() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		String string = transition.toString();
		assertTrue("Wrong string: " + string, string.contains("Transition"));
		assertTrue("Wrong string: " + string, string.contains("start"));
		assertTrue("Wrong string: " + string, string.contains("CONTIN???LE"));
		assertTrue("Wrong string: " + string, string.contains("next="));
	}

}
