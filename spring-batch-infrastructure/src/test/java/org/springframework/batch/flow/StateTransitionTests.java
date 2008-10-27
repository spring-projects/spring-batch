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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.flow.StateTransition;

/**
 * @author Dave Syer
 * 
 */
public class StateTransitionTests {

	@Test
	public void testIsEnd() {
		StateTransition<String> transition = StateTransition.createEndStateTransition(null, "");
		assertTrue(transition.isEnd());
		assertNull(transition.getNext());
	}

	@Test
	public void testMatchesStar() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesNull() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, null, "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesEmpty() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesExact() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTINUABLE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesWildcard() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN*", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testMatchesPlaceholder() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN???LE", "start");
		assertTrue(transition.matches("CONTINUABLE"));
	}

	@Test
	public void testSimpleOrderingEqual() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN???LE", "start");
		assertEquals(0, transition.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMoreGeneral() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN???LE", "start");
		StateTransition<String> other = StateTransition.createStateTransition(null, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostGeneral() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "*", "start");
		StateTransition<String> other = StateTransition.createStateTransition(null, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSubstringAndWildcard() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN*", "start");
		StateTransition<String> other = StateTransition.createStateTransition(null, "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostToNextGeneral() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "*", "start");
		StateTransition<String> other = StateTransition.createStateTransition(null, "C?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingAdjacent() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CON*", "start");
		StateTransition<String> other = StateTransition.createStateTransition(null, "CON?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testToString() {
		StateTransition<String> transition = StateTransition.createStateTransition(null, "CONTIN???LE", "start");
		String string = transition.toString();
		assertTrue("Wrong string: " + string, string.contains("Transition"));
		assertTrue("Wrong string: " + string, string.contains("start"));
		assertTrue("Wrong string: " + string, string.contains("CONTIN???LE"));
		assertTrue("Wrong string: " + string, string.contains("next="));
	}

}
