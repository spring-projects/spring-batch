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
	private List<Foo> generatedFoos = new ArrayList<Foo>(GENERATION_LIMIT);
	private List<Foo> processedFoos = new ArrayList<Foo>(GENERATION_LIMIT);
	private List<Foo> processedFooNameValuePairs = new ArrayList<Foo>(GENERATION_LIMIT);
	
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
