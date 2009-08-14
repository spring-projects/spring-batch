package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
	@Qualifier("withList")
	private PlaceholderTargetSource withList;

	@Autowired
	@Qualifier("withLiteralList")
	private PlaceholderTargetSource withLiteralList;

	@Autowired
	@Qualifier("withMap")
	private PlaceholderTargetSource withMap;

	@Autowired
	@Qualifier("withMultiple")
	private PlaceholderTargetSource withMultiple;

	@Autowired
	@Qualifier("withMultipleStartAndEnd")
	private PlaceholderTargetSource withMultipleStartAndEnd;

	@Autowired
	@Qualifier("withEmbeddedDate")
	private PlaceholderTargetSource withEmbeddedDate;

	@Autowired
	@Qualifier("withDate")
	private PlaceholderTargetSource withDate;

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

	public List<String> getList() {
		return Arrays.asList("bar", "spam");
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
	public void testGetList() {
		Node target = (Node) withList.getTarget();
		assertEquals(3, target.getList().size());
		assertEquals("[bar, foo-4321, bar-4321]", target.getList().toString());
	}

	@Test
	public void testGetLiteralList() {
		Node target = (Node) withLiteralList.getTarget();
		assertEquals(2, target.getList().size());
		assertEquals("[bar, spam]", target.getList().toString());
	}

	@Test
	public void testGetMap() {
		Node target = (Node) withMap.getTarget();
		assertEquals(3, target.getMap().size());
		assertEquals("{foo=bar, bar=foo-4321, spam=[bar, spam]}", target.getMap().toString());
	}

	@Test
	public void testGetMultiple() {
		Node target = (Node) withMultiple.getTarget();
		assertEquals("bar-4321-4321", target.getName());
	}

	@Test
	public void testGetMultipleStartAndEnd() {
		Node target = (Node) withMultipleStartAndEnd.getTarget();
		assertEquals("4321-4321", target.getName());
	}

	@Test
	public void testGetEmbeddedDate() {
		Node target = (Node) withEmbeddedDate.getTarget();
		String date = new Date(1L).toString();
		assertEquals("bar-"+date, target.getName());
	}

	@Test
	public void testGetDate() {
		Node target = (Node) withDate.getTarget();
		assertEquals(new Date(1L), target.getDate());
	}

	public static interface Node {
		String getName();

		Date getDate();

		Node getParent();
		
		List<String> getList();
		
		Map<String, Object> getMap();
	}

	public static class Foo implements Node {

		private String name;

		private Date date;

		private Node parent;
		
		private List<String> list;

		private Map<String, Object> map;

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

		public void setList(List<String> list) {
			this.list = list;
		}

		public List<String> getList() {
			return list;
		}

		public Map<String, Object> getMap() {
			return map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}

	}

}
