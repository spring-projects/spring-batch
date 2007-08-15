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

package org.springframework.batch.io.file.support.transform;

import org.springframework.batch.io.file.support.transform.FixedLengthLineAggregator;

import junit.framework.TestCase;

/**
 * Unit tests for {@link FixedLengthLineAggregator}
 * 
 * @author robert.kasanicky
 */
public class FixedLengthLineAggregatorTests extends TestCase {

	// object under test
	private FixedLengthLineAggregator aggregator = new FixedLengthLineAggregator();

	/**
	 * Record descriptor is null => BatchCriticalException
	 */
	public void testAggregateNullRecordDescriptor() {
		String[] args = { "does not matter what is here" };

		try {
			aggregator.aggregate(args);
			fail("should not work with null LineDescriptor");
		}
		catch (IllegalArgumentException expected) {
			// expected
		}
	}

	/**
	 * Argument count does not match the number of fields in the
	 * LineDescriptor
	 */
	public void testAggregateWrongArgumentCount() {
		String[] args = { "only one argument" };
		aggregator.setLengths(new int[0]);

		try {
			aggregator.aggregate(args);
			fail("Wrong argument count, exception exptected");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(true);
		}
	}

	/**
	 * Argument length exceeds the length specified by FieldDescriptor
	 */
	public void testAggregateInvalidInputLength() {
		String[] args = { "Oversize" };
		aggregator.setLengths(new int[] {args[0].length()-1});
		try {
			aggregator.aggregate(args);
			fail("Invalid argument length, exception should have been thrown");
		}
		catch (IllegalArgumentException expected) {
			// expected
		}
	}

	/**
	 * Regular use with valid LineDescriptor
	 */
	public void testAggregate() {
		String[] args = { "Matchsize", "Smallsize" };
		aggregator.setLengths(new int[] {args[0].length(), args[1].length()});
		String result = aggregator.aggregate(args);
		assertEquals("MatchsizeSmallsize", result);
	}

	/**
	 * Regular use with valid LineDescriptor
	 */
	public void testAggregateFormattedRight() {
		String[] args = { "Matchsize", "Smallsize" };
		aggregator.setAlignment("right");
		aggregator.setLengths(new int[] {args[0].length()+4, args[1].length()+1});
		String result = aggregator.aggregate(args);
		assertEquals(result, "    Matchsize Smallsize");
	}

	/**
	 * Regular use with valid LineDescriptor
	 */
	public void testAggregateFormattedCenter() {
		String[] args = { "Matchsize", "Smallsize" };
		aggregator.setAlignment("center");
		aggregator.setLengths(new int[] {args[0].length()+4, args[1].length()+1});
		String result = aggregator.aggregate(args);
		assertEquals(result, "  Matchsize  Smallsize ");
	}

	/**
	 * If one of the passed arguments is null, string filled with spaces should
	 * be returned
	 */
	public void testAggregateNullArgument() {
		String[] args = { null };

		aggregator.setLengths(new int[] {3});

		try {
			assertEquals("   ", aggregator.aggregate(args));
		}
		catch (NullPointerException unexpected) {
			fail("incorrect handling of null arguments");
		}
	}
}
