/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.step.skip;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemWriterException;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class LimitCheckingItemSkipPolicyTests {

	private LimitCheckingItemSkipPolicy failurePolicy;

	@Before
	public void setUp() throws Exception {
		List<Class<? extends Throwable>> skippableExceptions = new ArrayList<Class<? extends Throwable>>();
		skippableExceptions.add(FlatFileParseException.class);
		List<Class<? extends Throwable>> fatalExceptions = new ArrayList<Class<? extends Throwable>>();
		failurePolicy = new LimitCheckingItemSkipPolicy(1, skippableExceptions, fatalExceptions);
	}

	@Test
	public void testLimitExceed() {
		try {
			failurePolicy.shouldSkip(new FlatFileParseException("", ""), 2);
			fail();
		} catch (SkipLimitExceededException ex) {
			// expected
		}
	}

	@Test
	public void testNonSkippableException() {
		assertFalse(failurePolicy.shouldSkip(new FileNotFoundException(), 2));
	}

	@Test
	public void testSkip() {
		assertTrue(failurePolicy.shouldSkip(new FlatFileParseException("", ""), 0));
	}

	private LimitCheckingItemSkipPolicy getSkippableSubsetFailurePolicy() {
		List<Class<? extends Throwable>> skippableExceptions = new ArrayList<Class<? extends Throwable>>();
		skippableExceptions.add(WriteFailedException.class);
		List<Class<? extends Throwable>> fatalExceptions = new ArrayList<Class<? extends Throwable>>();
		fatalExceptions.add(ItemWriterException.class);
		return new LimitCheckingItemSkipPolicy(1, skippableExceptions, fatalExceptions);
	}

	/**
	 * condition: skippable < fatal; exception is unclassified
	 * 
	 * expected: false; default classification
	 */
	@Test
	public void testSkippableSubset_unclassified() {
		assertFalse(getSkippableSubsetFailurePolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: skippable < fatal; exception is skippable
	 * 
	 * expected: false; fatal overrides skippable
	 */
	@Test
	public void testSkippableSubset_skippable() {
		assertFalse(getSkippableSubsetFailurePolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	/**
	 * condition: skippable < fatal; exception is fatal
	 * 
	 * expected: false
	 */
	@Test
	public void testSkippableSubset_fatal() {
		assertFalse(getSkippableSubsetFailurePolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	private LimitCheckingItemSkipPolicy getFatalSubsetFailurePolicy() {
		List<Class<? extends Throwable>> skippableExceptions = new ArrayList<Class<? extends Throwable>>();
		skippableExceptions.add(ItemWriterException.class);
		List<Class<? extends Throwable>> fatalExceptions = new ArrayList<Class<? extends Throwable>>();
		fatalExceptions.add(WriteFailedException.class);
		return new LimitCheckingItemSkipPolicy(1, skippableExceptions, fatalExceptions);
	}

	/**
	 * condition: fatal < skippable; exception is unclassified
	 * 
	 * expected: false; default classification
	 */
	@Test
	public void testFatalSubset_unclassified() {
		assertFalse(getFatalSubsetFailurePolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: fatal < skippable; exception is skippable
	 * 
	 * expected: true
	 */
	@Test
	public void testFatalSubset_skippable() {
		assertTrue(getFatalSubsetFailurePolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is fatal
	 * 
	 * expected: false
	 */
	@Test
	public void testFatalSubset_fatal() {
		assertFalse(getFatalSubsetFailurePolicy().shouldSkip(new WriteFailedException(""), 0));
	}
}
