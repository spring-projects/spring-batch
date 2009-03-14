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
package org.springframework.batch.retry.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.classify.Classifier;

/**
 * @author Dave Syer
 * 
 */
public class DefaultRetryStateTests {

	/**
	 * Test method for
	 * {@link org.springframework.batch.retry.support.DefaultRetryState#DefaultRetryState(java.lang.Object, boolean, org.springframework.batch.classify.Classifier)}.
	 */
	@Test
	public void testDefaultRetryStateObjectBooleanClassifierOfQsuperThrowableBoolean() {
		DefaultRetryState state = new DefaultRetryState("foo", true, new Classifier<Throwable, Boolean>() {
			public Boolean classify(Throwable classifiable) {
				return false;
			}
		});
		assertEquals("foo", state.getKey());
		assertTrue(state.isForceRefresh());
		assertFalse(state.rollbackFor(null));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.retry.support.DefaultRetryState#DefaultRetryState(java.lang.Object, org.springframework.batch.classify.Classifier)}.
	 */
	@Test
	public void testDefaultRetryStateObjectClassifierOfQsuperThrowableBoolean() {
		DefaultRetryState state = new DefaultRetryState("foo", new Classifier<Throwable, Boolean>() {
			public Boolean classify(Throwable classifiable) {
				return false;
			}
		});
		assertEquals("foo", state.getKey());
		assertFalse(state.isForceRefresh());
		assertFalse(state.rollbackFor(null));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.retry.support.DefaultRetryState#DefaultRetryState(java.lang.Object, boolean)}.
	 */
	@Test
	public void testDefaultRetryStateObjectBoolean() {
		DefaultRetryState state = new DefaultRetryState("foo", true);
		assertEquals("foo", state.getKey());
		assertTrue(state.isForceRefresh());
		assertTrue(state.rollbackFor(null));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.retry.support.DefaultRetryState#DefaultRetryState(java.lang.Object)}.
	 */
	@Test
	public void testDefaultRetryStateObject() {
		DefaultRetryState state = new DefaultRetryState("foo");
		assertEquals("foo", state.getKey());
		assertFalse(state.isForceRefresh());
		assertTrue(state.rollbackFor(null));
	}

}
