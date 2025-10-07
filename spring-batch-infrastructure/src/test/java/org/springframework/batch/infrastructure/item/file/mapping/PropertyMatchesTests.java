/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.file.mapping;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.mapping.PropertyMatches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyMatchesTests {

	@Test
	void testPropertyMatchesWithMaxDistance() {
		String[] matches = PropertyMatches.forProperty("DUCK_SOUP", PropertyBean.class, 2).getPossibleMatches();
		assertEquals(1, matches.length);
	}

	@Test
	void testPropertyMatchesWithDefault() {
		String[] matches = PropertyMatches.forProperty("DUCK_SOUP", PropertyBean.class).getPossibleMatches();
		assertEquals(1, matches.length);
	}

	@Test
	void testBuildErrorMessageNoMatches() {
		String msg = PropertyMatches.forProperty("foo", PropertyBean.class, 2).buildErrorMessage();
		assertTrue(msg.contains("foo"));
	}

	@Test
	void testBuildErrorMessagePossibleMatch() {
		String msg = PropertyMatches.forProperty("DUCKSOUP", PropertyBean.class, 1).buildErrorMessage();
		// the message contains the close match
		assertTrue(msg.contains("duckSoup"));
	}

	@Test
	void testBuildErrorMessageMultiplePossibleMatches() {
		String msg = PropertyMatches.forProperty("DUCKCRAP", PropertyBean.class, 4).buildErrorMessage();
		// the message contains the close matches
		assertTrue(msg.contains("duckSoup"));
		assertTrue(msg.contains("duckPate"));
	}

	@Test
	void testEmptyString() {
		String[] matches = PropertyMatches.forProperty("", PropertyBean.class, 4).getPossibleMatches();
		assertEquals("name", matches[0]);
	}

	private static class BaseBean {

		public void setName(String name) {
		}

	}

	private static class PropertyBean extends BaseBean {

		public void setDuckSoup(String duckSoup) {
		}

		public void setDuckPate(String duckPate) {
		}

		public void setDuckBreast(String duckBreast) {
		}

	}

}
