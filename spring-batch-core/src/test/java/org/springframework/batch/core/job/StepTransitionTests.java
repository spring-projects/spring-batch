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
package org.springframework.batch.core.job;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class StepTransitionTests {

	@Test
	public void testIsEnd() {
		StepTransition transition = new StepTransition(new StepSupport(), "");
		assertTrue(transition.isEnd());
		assertNull(transition.getNext());
	}

	@Test
	public void testMatchesStar() {
		StepTransition transition = new StepTransition(new StepSupport(), "*", "start");
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testMatchesNull() {
		StepTransition transition = new StepTransition(new StepSupport(), null, "start");
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testMatchesEmpty() {
		StepTransition transition = new StepTransition(new StepSupport(), "", "start");
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testMatchesExact() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTINUABLE", "start");
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testMatchesWildcard() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN*", "start" );
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testMatchesPlaceholder() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN???LE", "start");
		assertTrue(transition.matches(ExitStatus.CONTINUABLE));
	}

	@Test
	public void testSimpleOrderingEqual() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN???LE", "start");
		assertEquals(0, transition.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMoreGeneral() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN???LE", "start");
		StepTransition other = new StepTransition(new StepSupport(), "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostGeneral() {
		StepTransition transition = new StepTransition(new StepSupport(), "*", "start");
		StepTransition other = new StepTransition(new StepSupport(), "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSubstringAndWildcard() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN*", "start");
		StepTransition other = new StepTransition(new StepSupport(), "CONTINUABLE", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingMostToNextGeneral() {
		StepTransition transition = new StepTransition(new StepSupport(), "*", "start");
		StepTransition other = new StepTransition(new StepSupport(), "C?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testSimpleOrderingAdjacent() {
		StepTransition transition = new StepTransition(new StepSupport(), "CON*", "start");
		StepTransition other = new StepTransition(new StepSupport(), "CON?", "start");
		assertEquals(1, transition.compareTo(other));
		assertEquals(-1, other.compareTo(transition));
	}

	@Test
	public void testToString() {
		StepTransition transition = new StepTransition(new StepSupport(), "CONTIN???LE", "start");
		String string = transition.toString();
		assertTrue("Wrong string: " + string, string.contains("StepTransition"));
		assertTrue("Wrong string: " + string, string.contains("start"));
		assertTrue("Wrong string: " + string, string.contains("CONTIN???LE"));
		assertTrue("Wrong string: " + string, string.contains("next="));
	}

}
