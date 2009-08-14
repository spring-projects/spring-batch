package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class PlaceholderTargetSourceErrorTests extends ContextFactorySupport {

	private Map<String, Object> map = Collections.singletonMap("foo.foo", (Object) "bar");

	private Date date = new Date(1L);

	public Object getContext() {
		return this;
	}

	public String getFoo() {
		return "bar";
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public Node getParent() {
		return new Foo("spam");
	}

	public Long getLong() {
		return 12345678912345L;
	}

	public Integer getInteger() {
		return 4321;
	}

	public Date getDate() {
		return date;
	}

	public String getGarbage() {
		return null;
	}

	private PlaceholderTargetSource createValue(String name, String value) throws Exception {
		String input = IOUtils.toString(new ClassPathResource(getClass().getSimpleName() + "-context.xml", getClass())
				.getInputStream());
		input = input.replace("<!-- INSERT -->", String.format("<property name=\"%s\" value=\"%s\" />", name, value));
		Resource resource = new ByteArrayResource(input.getBytes());
		GenericApplicationContext context = new GenericApplicationContext();
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(resource);
		context.refresh();
		// XmlBeanFactory context = new XmlBeanFactory(resource);
		return (PlaceholderTargetSource) context.getBean("value");
	}

	@Test
	public void testPartialReplaceSunnyDay() throws Exception {
		Node target = (Node) createValue("name", "%{foo}-bar").getTarget();
		assertEquals("bar-bar", target.getName());
	}

	@Test
	public void testPartialReplaceMissingProperty() throws Exception {
		try {
			Node target = (Node) createValue("name", "%{garbage}-bar").getTarget();
			assertEquals("%{garbage}-bar", target.getName());
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.toLowerCase().contains("cannot bind"));
		}
	}

	@Test
	public void testFullReplaceSunnyDay() throws Exception {
		Node target = (Node) createValue("name", "%{foo}").getTarget();
		assertEquals("bar", target.getName());
	}

	@Test
	public void testFullReplaceMissingProperty() throws Exception {
		try {
			Node target = (Node) createValue("name", "%{garbage}").getTarget();
			assertEquals("bar", target.getName());
			fail("Expected IllegalStateException");
		}
		catch (BeanCreationException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.toLowerCase().contains("cannot bind"));
		}
	}

	@Test
	public void testPartialReplaceIntegerToString() throws Exception {
			Node target = (Node) createValue("name", "foo-%{integer}").getTarget();
			assertEquals("foo-4321", target.getName());
	}

	@Test
	public void testFullReplaceIntegerToString() throws Exception {
		Node target = (Node) createValue("name", "%{integer}").getTarget();
		assertEquals("4321", target.getName());
	}

	@Test
	public void testFullReplaceIntegerToLong() throws Exception {
		Node target = (Node) createValue("value", "%{integer}").getTarget();
		assertEquals(4321L, target.getValue());
	}

	@Test
	public void testFullReplaceIntegerToNode() throws Exception {
		try {
			Node target = (Node) createValue("parent", "%{integer}").getTarget();
			assertEquals("4321", target.getParent());
			fail("Expected IllegalArgumentException");
		}
		catch (Exception e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.toLowerCase().contains("cannot convert"));
		}
	}

	public static interface Node {
		String getName();

		Date getDate();

		Node getParent();

		long getValue();
	}

	public static class Foo implements Node {

		private String name;

		private Date date;

		private Node parent;

		private long value;

		public Foo() {
		}

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

	}

}
