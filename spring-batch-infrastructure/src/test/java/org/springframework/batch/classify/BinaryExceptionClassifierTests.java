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

package org.springframework.batch.classify;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

public class BinaryExceptionClassifierTests extends TestCase {

	BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(false);

	public void testClassifyNullIsDefault() {
		assertFalse(classifier.classify(null));
	}

	public void testFalseIsDefault() {
		assertFalse(classifier.getDefault());
	}

	public void testDefaultProvided() {
		classifier = new BinaryExceptionClassifier(true);
		assertTrue(classifier.getDefault());
	}

	public void testClassifyRandomException() {
		assertFalse(classifier.classify(new IllegalStateException("foo")));
	}

	public void testClassifyExactMatch() {
		Collection<Class<? extends Throwable>> set = Collections
				.<Class<? extends Throwable>> singleton(IllegalStateException.class);
		assertTrue(new BinaryExceptionClassifier(set).classify(new IllegalStateException("Foo")));
	}

	public void testTypesProvidedInConstructor() {
		classifier = new BinaryExceptionClassifier(Collections
				.<Class<? extends Throwable>> singleton(IllegalStateException.class));
		assertTrue(classifier.classify(new IllegalStateException("Foo")));
	}

	public void testTypesProvidedInConstructorWithNonDefault() {
		classifier = new BinaryExceptionClassifier(Collections
				.<Class<? extends Throwable>> singleton(IllegalStateException.class), false);
		assertFalse(classifier.classify(new IllegalStateException("Foo")));
	}
}
