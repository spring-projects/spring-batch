/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.step.skip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ItemWriterException;
import org.springframework.batch.infrastructure.item.WriteFailedException;
import org.springframework.batch.infrastructure.item.WriterNotOpenException;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

/**
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
class LimitCheckingItemSkipPolicyTests {

	private LimitCheckingItemSkipPolicy failurePolicy;

	@BeforeEach
	void setUp() {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<>();
		skippableExceptions.put(FlatFileParseException.class, true);
		failurePolicy = new LimitCheckingItemSkipPolicy(1, skippableExceptions);
	}

	@Test
	void testLimitExceed() {
		assertThrows(SkipLimitExceededException.class,
				() -> failurePolicy.shouldSkip(new FlatFileParseException("", ""), 2));
	}

	@Test
	void testNonSkippableException() {
		assertFalse(failurePolicy.shouldSkip(new FileNotFoundException(), 2));
	}

	@Test
	void testSkip() {
		assertTrue(failurePolicy.shouldSkip(new FlatFileParseException("", ""), 0));
	}

	private LimitCheckingItemSkipPolicy getSkippableSubsetSkipPolicy() {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<>();
		skippableExceptions.put(WriteFailedException.class, true);
		skippableExceptions.put(ItemWriterException.class, false);
		return new LimitCheckingItemSkipPolicy(1, skippableExceptions);
	}

	/**
	 * condition: skippable < fatal; exception is unclassified
	 * <p>
	 * expected: false; default classification
	 */
	@Test
	void testSkippableSubset_unclassified() {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: skippable < fatal; exception is skippable
	 * <p>
	 * expected: true
	 */
	@Test
	void testSkippableSubset_skippable() {
		assertTrue(getSkippableSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	/**
	 * condition: skippable < fatal; exception is fatal
	 * <p>
	 * expected: false
	 */
	@Test
	void testSkippableSubset_fatal() {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	private LimitCheckingItemSkipPolicy getFatalSubsetSkipPolicy() {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<>();
		skippableExceptions.put(WriteFailedException.class, false);
		skippableExceptions.put(ItemWriterException.class, true);
		return new LimitCheckingItemSkipPolicy(1, skippableExceptions);
	}

	/**
	 * condition: fatal < skippable; exception is unclassified
	 * <p>
	 * expected: false; default classification
	 */
	@Test
	void testFatalSubset_unclassified() {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: fatal < skippable; exception is skippable
	 * <p>
	 * expected: true
	 */
	@Test
	void testFatalSubset_skippable() {
		assertTrue(getFatalSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is fatal
	 * <p>
	 * expected: false
	 */
	@Test
	void testFatalSubset_fatal() {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

}
