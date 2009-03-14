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

import org.springframework.batch.classify.ClassifierSupport;

import junit.framework.TestCase;

public class ClassifierSupportTests extends TestCase {

	public void testClassifyNullIsDefault() {
		ClassifierSupport<String,String> classifier = new ClassifierSupport<String,String>("foo");
		assertEquals(classifier.classify(null), "foo");
	}

	public void testClassifyRandomException() {
		ClassifierSupport<Throwable,String> classifier = new ClassifierSupport<Throwable,String>("foo");
		assertEquals(classifier.classify(new IllegalStateException("Foo")), classifier.classify(null));
	}

}
