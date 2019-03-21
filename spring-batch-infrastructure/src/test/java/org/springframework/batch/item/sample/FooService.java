/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.batch.item.sample;

import java.util.ArrayList;
import java.util.List;


/**
 * Custom class that contains the logic of providing and processing {@link Foo}
 * objects. It serves the purpose to show how providing/processing logic contained in a
 * custom class can be reused by the framework.
 * 
 * @author Robert Kasanicky
 */
public class FooService {

	public static final int GENERATION_LIMIT = 10;
	
	private int counter = 0;
	private List<Foo> generatedFoos = new ArrayList<>(GENERATION_LIMIT);
	private List<Foo> processedFoos = new ArrayList<>(GENERATION_LIMIT);
	private List<Foo> processedFooNameValuePairs = new ArrayList<>(GENERATION_LIMIT);
	
	public Foo generateFoo() {
		if (counter++ >= GENERATION_LIMIT) return null;
		
		Foo foo = new Foo(counter, "foo" + counter, counter);
		generatedFoos.add(foo);
		return foo;
	
	}
	
	public void processFoo(Foo foo) {
		processedFoos.add(foo);
	}
	
	public String extractName(Foo foo) {
		processedFoos.add(foo);
		return foo.getName();
	}
	
	public void processNameValuePair(String name, int value) {
		processedFooNameValuePairs.add(new Foo(0, name, value));
	}

	public List<Foo> getGeneratedFoos() {
		return generatedFoos;
	}

	public List<Foo> getProcessedFoos() {
		return processedFoos;
	}

	public List<Foo> getProcessedFooNameValuePairs() {
		return processedFooNameValuePairs;
	}	
	
}
