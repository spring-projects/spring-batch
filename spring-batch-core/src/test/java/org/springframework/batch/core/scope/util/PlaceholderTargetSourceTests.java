package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PlaceholderTargetSourceTests extends ContextFactorySupport {

	@Autowired
	@Qualifier("vanilla")
	private PlaceholderTargetSource vanilla;

	@Autowired
	@Qualifier("simple")
	private PlaceholderTargetSource simple;

	@Autowired
	@Qualifier("withLong")
	private PlaceholderTargetSource withLong;

	@Autowired
	@Qualifier("withInteger")
	private PlaceholderTargetSource withInteger;

	@Autowired
	@Qualifier("withMultiple")
	private PlaceholderTargetSource withMultiple;

	@Autowired
	@Qualifier("withEmbeddedDate")
	private PlaceholderTargetSource withEmbeddedDate;

	@Autowired
	@Qualifier("withDate")
	private PlaceholderTargetSource withDate;

	@Autowired
	@Qualifier("withNull")
	private PlaceholderTargetSource withNull;

	@Autowired
	@Qualifier("compound")
	private PlaceholderTargetSource compound;

	@Autowired
	@Qualifier("ref")
	private PlaceholderTargetSource ref;

	@Autowired
	@Qualifier("value")
	private PlaceholderTargetSource value;

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

	@Test
	public void testAfterPropertiesSet() throws Exception {
		PlaceholderTargetSource targetSource = new PlaceholderTargetSource();
		try {
			targetSource.afterPropertiesSet();
			fail("Axpected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testGetVanilla() {
		Node target = (Node) vanilla.getTarget();
		assertEquals("foo", target.getName());
	}

	@Test
	public void testGetSimple() {
		Node target = (Node) simple.getTarget();
		assertEquals("bar", target.getName());
	}

	@Test
	public void testGetCompound() {
		Node target = (Node) compound.getTarget();
		assertEquals("bar-bar", target.getName());
	}

	@Test
	public void testGetRef() {
		Node target = (Node) ref.getTarget();
		assertEquals("foo", target.getParent().getName());
	}

	@Test
	public void testGetValue() {
		Node target = (Node) value.getTarget();
		assertEquals("spam", target.getParent().getName());
	}

	@Test
	public void testGetLong() {
		Node target = (Node) withLong.getTarget();
		assertEquals("bar-12345678912345", target.getName());
	}

	@Test
	public void testGetInteger() {
		Node target = (Node) withInteger.getTarget();
		assertEquals("bar-4321", target.getName());
	}

	@Test
	public void testGetMultiple() {
		Node target = (Node) withMultiple.getTarget();
		assertEquals("bar-4321-4321", target.getName());
	}

	@Test
	public void testGetEmbeddedDate() {
		Node target = (Node) withEmbeddedDate.getTarget();
		assertEquals("bar-1970/01/01", target.getName());
	}

	@Test
	public void testGetDate() {
		Node target = (Node) withDate.getTarget();
		assertEquals(1L, target.getDate().getTime());
	}

	@Test
	public void testGetNull() {
		Node target = (Node) withNull.getTarget();
		// Remains unconverted because null is explicitly excluded
		assertEquals("bar-#{garbage}", target.getName());
	}

	public static interface Node {
		String getName();

		Date getDate();

		Node getParent();
	}

	public static class Foo implements Node {

		private String name;

		private Date date;

		private Node parent;

		public Foo() {
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
