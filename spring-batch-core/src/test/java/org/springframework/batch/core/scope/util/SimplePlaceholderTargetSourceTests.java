package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SimplePlaceholderTargetSourceTests {
	
	@Autowired
	@Qualifier("simple")
	private Node simple;
	
	@Autowired
	private SimpleContextFactory contextFactory;
	
	@Test
	public void testGetSimple() {
		contextFactory.set("bar");
		assertEquals("bar", simple.getName());
		contextFactory.clear();
	}
	
	public static class SimpleContextFactory extends ContextFactorySupport {

		private ThreadLocal<String> fooHolder = new ThreadLocal<String>();
		
		public Object getContext() {
			return this;
		}

		public void set(String value) {
			fooHolder.set(value);
		}

		public void clear() {
			fooHolder.set(null);
		}

		public String getFoo() {
			return fooHolder.get();
		}
		
	}

	public static interface Node {
		String getName();
	}
	
	public static class Foo implements Node {

		private String name;
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
	}

}
