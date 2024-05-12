/*
 * Copyright 2006-2024 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.StateSupport;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Kim Youngwoong
 */
class StateTransitionTests {

	State state = new StateSupport("state1");

	@Test
	void testIsEnd() {
		StateTransition transition = StateTransition.createEndStateTransition(state, "");
		assertTrue(transition.isEnd());
		assertNull(transition.getNext());
	}

	@Test
	void testMatchesStar() {
		StateTransition transition = StateTransition.createStateTransition(state, "*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testMatchesNull() {
		StateTransition transition = StateTransition.createStateTransition(state, null, "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testMatchesEmpty() {
		StateTransition transition = StateTransition.createStateTransition(state, "", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testMatchesExact() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTINUABLE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testMatchesWildcard() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testMatchesPlaceholder() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	void testEquals() {
		StateTransition transition1 = StateTransition.createStateTransition(state, "pattern1", "next1");
		StateTransition transition2 = StateTransition.createStateTransition(state, "pattern1", "next1");
		StateTransition transition3 = StateTransition.createStateTransition(state, "pattern2", "next2");

		assertEquals(transition1, transition2);
		assertNotEquals(transition1, transition3);
		assertEquals(transition1, transition1);
		assertNotEquals(null, transition1);
	}

	@Test
	void testToString() {
		StateTransition transition = StateTransition.createStateTransition(state, "CONTIN???LE", "start");
		String string = transition.toString();
		assertTrue(string.contains("Transition"), "Wrong string: " + string);
		assertTrue(string.contains("start"), "Wrong string: " + string);
		assertTrue(string.contains("CONTIN???LE"), "Wrong string: " + string);
		assertTrue(string.contains("next="), "Wrong string: " + string);
	}

}
