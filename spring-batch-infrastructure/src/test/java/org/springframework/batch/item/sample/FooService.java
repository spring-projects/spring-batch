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
	private List generatedFoos = new ArrayList(GENERATION_LIMIT);
	private List processedFoos = new ArrayList(GENERATION_LIMIT);
	private List processedFooNameValuePairs = new ArrayList(GENERATION_LIMIT);
	
	public Foo generateFoo() {
		if (counter++ >= GENERATION_LIMIT) return null;
		
		Foo foo = new Foo(counter, "foo" + counter, counter);
		generatedFoos.add(foo);
		return foo;
	
	}
	
	public void processFoo(Foo foo) {
		processedFoos.add(foo);
	}
	
	public void processNameValuePair(String name, int value) {
		processedFooNameValuePairs.add(new Foo(0, name, value));
	}

	public List getGeneratedFoos() {
		return generatedFoos;
	}

	public List getProcessedFoos() {
		return processedFoos;
	}

	public List getProcessedFooNameValuePairs() {
		return processedFooNameValuePairs;
	}
	
	
}
