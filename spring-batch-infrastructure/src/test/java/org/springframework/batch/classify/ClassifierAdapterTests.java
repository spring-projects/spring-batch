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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.classify.ClassifierAdapter;
import org.springframework.batch.support.annotation.Classifier;

/**
 * @author Dave Syer
 * 
 */
public class ClassifierAdapterTests {

	private ClassifierAdapter<String, Integer> adapter = new ClassifierAdapter<String, Integer>();

	@Test
	public void testClassifierAdapterObject() {
		adapter = new ClassifierAdapter<String, Integer>(new Object() {
			@SuppressWarnings("unused")
			@Classifier
			public Integer getValue(String key) {
				return Integer.parseInt(key);
			}

			@SuppressWarnings("unused")
			public Integer getAnother(String key) {
				throw new UnsupportedOperationException("Not allowed");
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test(expected = IllegalStateException.class)
	public void testClassifierAdapterObjectWithNoAnnotation() {
		adapter = new ClassifierAdapter<String, Integer>(new Object() {
			@SuppressWarnings("unused")
			public Integer getValue(String key) {
				return Integer.parseInt(key);
			}

			@SuppressWarnings("unused")
			public Integer getAnother(String key) {
				throw new UnsupportedOperationException("Not allowed");
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test
	public void testClassifierAdapterObjectSingleMethodWithNoAnnotation() {
		adapter = new ClassifierAdapter<String, Integer>(new Object() {
			@SuppressWarnings("unused")
			public Integer getValue(String key) {
				return Integer.parseInt(key);
			}
			@SuppressWarnings("unused")
			public void doNothing(String key) {
			}
			@SuppressWarnings("unused")
			public String doNothing(String key, int value) {
				return "foo";
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test
	public void testClassifierAdapterClassifier() {
		adapter = new ClassifierAdapter<String, Integer>(
				new org.springframework.batch.classify.Classifier<String, Integer>() {
					public Integer classify(String classifiable) {
						return Integer.valueOf(classifiable);
					}
				});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test
	public void testClassifyWithSetter() {
		adapter.setDelegate(new Object() {
			@SuppressWarnings("unused")
			@Classifier
			public Integer getValue(String key) {
				return Integer.parseInt(key);
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testClassifyWithWrongType() {
		adapter.setDelegate(new Object() {
			@SuppressWarnings("unused")
			@Classifier
			public String getValue(Integer key) {
				return key.toString();
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

	@Test
	public void testClassifyWithClassifier() {
		adapter.setDelegate(new org.springframework.batch.classify.Classifier<String, Integer>() {
			public Integer classify(String classifiable) {
				return Integer.valueOf(classifiable);
			}
		});
		assertEquals(23, adapter.classify("23").intValue());
	}

}
