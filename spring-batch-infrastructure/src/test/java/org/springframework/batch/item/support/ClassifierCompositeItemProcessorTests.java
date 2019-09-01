/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.batch.item.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.PatternMatchingClassifier;
import org.springframework.classify.SubclassClassifier;
import org.springframework.lang.Nullable;

/**
 * @author Jimmy Praet
 */
public class ClassifierCompositeItemProcessorTests {
	
	@Test
	public void testBasicClassifierCompositeItemProcessor() throws Exception {
		ClassifierCompositeItemProcessor<String, String> processor = new ClassifierCompositeItemProcessor<>();
		
		ItemProcessor<String, String> fooProcessor = new ItemProcessor<String, String>() {
			@Nullable
			@Override
			public String process(String item) throws Exception {
				return "foo: " + item;
			}
		};
		ItemProcessor<String, String> defaultProcessor = new ItemProcessor<String, String>() {
			@Nullable
			@Override
			public String process(String item) throws Exception {
				return item;
			}
		};
		
		Map<String, ItemProcessor<?, ? extends String>> routingConfiguration = 
				new HashMap<>();
		routingConfiguration.put("foo", fooProcessor);
		routingConfiguration.put("*", defaultProcessor);
		processor.setClassifier(new PatternMatchingClassifier<>(routingConfiguration));
		
		assertEquals("bar", processor.process("bar"));
		assertEquals("foo: foo", processor.process("foo"));
		assertEquals("baz", processor.process("baz"));
	}

	/**
	 * Test the ClassifierCompositeItemProcessor with delegates that have more specific generic types for input as well as output.
	 */
	@Test
	public void testGenericsClassifierCompositeItemProcessor() throws Exception {
		ClassifierCompositeItemProcessor<Number, CharSequence> processor = new ClassifierCompositeItemProcessor<>();
		
		ItemProcessor<Integer, String> intProcessor = new ItemProcessor<Integer, String>() {
			@Nullable
			@Override
			public String process(Integer item) throws Exception {
				return "int: " + item;
			}
		};
		ItemProcessor<Long, StringBuffer> longProcessor = new ItemProcessor<Long, StringBuffer>() {
			@Nullable
			@Override
			public StringBuffer process(Long item) throws Exception {
				return new StringBuffer("long: " + item);
			}
		};
		ItemProcessor<Number, StringBuilder> defaultProcessor = new ItemProcessor<Number, StringBuilder>() {
			@Nullable
			@Override
			public StringBuilder process(Number item) throws Exception {
				return new StringBuilder("number: " + item);
			}
		};
		
		SubclassClassifier<Number, ItemProcessor<?, ? extends CharSequence>> classifier = 
				new SubclassClassifier<>();
		Map<Class<? extends Number>, ItemProcessor<?, ? extends CharSequence>> typeMap = 
				new HashMap<>();
		typeMap.put(Integer.class, intProcessor);
		typeMap.put(Long.class, longProcessor);
		typeMap.put(Number.class, defaultProcessor);
		classifier.setTypeMap(typeMap);
		processor.setClassifier(classifier);
		
		assertEquals("int: 1", processor.process(Integer.valueOf(1)).toString());
		assertEquals("long: 2", processor.process(Long.valueOf(2)).toString());
		assertEquals("number: 3", processor.process(Byte.valueOf((byte) 3)).toString());
	}
	
}
