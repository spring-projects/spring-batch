package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PlaceholderTargetSourceCustomEditorTests extends ContextFactorySupport {

	@Autowired
	@Qualifier("withEmbeddedDate")
	private PlaceholderTargetSource withEmbeddedDate;

	private Date date = new Date(1L);

	public Object getContext() {
		return this;
	}

	public Date getDate() {
		return date;
	}

	@Test
	public void testGetEmbeddedDate() {
		Node target = (Node) withEmbeddedDate.getTarget();
		String date = new SimpleDateFormat("yyyy/MM/dd").format(new Date(1L));
		assertEquals("bar-"+date, target.getName());
	}

	public static interface Node {
		String getName();
	}

	public static class Foo implements Node {

		private String name;

		public Foo() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
