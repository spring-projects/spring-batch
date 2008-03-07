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

package org.springframework.batch.item.file.mapping;

import junit.framework.TestCase;

public class PropertyMatchesTests extends TestCase {
	
	public void setDuckSoup(String duckSoup) {
	}
	
	public void setDuckPate(String duckPate) {
	}

	public void setDuckBreast(String duckBreast) {
	}

	public void testPropertyMatchesWithMaxDistance() throws Exception {
		String[] matches = PropertyMatches.forProperty("DUCK_SOUP", getClass(), 2).getPossibleMatches();
		assertEquals(1, matches.length);
	}
	
	public void testPropertyMatchesWithDefault() throws Exception {
		String[] matches = PropertyMatches.forProperty("DUCK_SOUP", getClass()).getPossibleMatches();
		assertEquals(1, matches.length);
	}

	public void testBuildErrorMessageNoMatches() throws Exception {
		String msg = PropertyMatches.forProperty("foo", getClass(), 2).buildErrorMessage();
		assertTrue(msg.indexOf("foo")>=0);
	}

	public void testBuildErrorMessagePossibleMatch() throws Exception {
		String msg = PropertyMatches.forProperty("DUCKSOUP", getClass(), 1).buildErrorMessage();
		// the message contains the close match 
		assertTrue(msg.indexOf("duckSoup")>=0);
	}

	public void testBuildErrorMessageMultiplePossibleMatches() throws Exception {
		String msg = PropertyMatches.forProperty("DUCKCRAP", getClass(), 4).buildErrorMessage();
		// the message contains the close matches
		assertTrue(msg.indexOf("duckSoup")>=0);
		assertTrue(msg.indexOf("duckPate")>=0);
	}
	
	public void testEmptyString() throws Exception {
		String[] matches = PropertyMatches.forProperty("", getClass(), 4).getPossibleMatches();
		// TestCase base class has a name property
		assertEquals("name", matches[0]);
	}
}
