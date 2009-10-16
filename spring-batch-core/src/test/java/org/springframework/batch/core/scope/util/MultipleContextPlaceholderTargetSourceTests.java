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
package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MultipleContextPlaceholderTargetSourceTests {

	private Map<String, String> attributes;

	public Map<String, String> getAttributes() {
		return attributes;
	}

	@Autowired
	private SimpleContextFactory contextFactory;

	@Autowired
	@Qualifier("simple")
	private TestBean simple;

	@Autowired
	@Qualifier("nested")
	private TestBean nested;

	@Autowired
	@Qualifier("list")
	private TestBean list;

	@Autowired
	@Qualifier("nestedList")
	private TestBean nestedList;

	@Autowired
	@Qualifier("map")
	private TestBean map;

	@After
	public void removeContext() {
		contextFactory.clearContext();
	}

	@Before
	public void setUpContext() {
		contextFactory.setContext(this);
	}

	@Test
	public void testValueFromProperties() throws Exception {
		attributes = Collections.singletonMap("foo", "bar");
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testMultipleValueFromProperties() throws Exception {

		for (int i = 0; i < 4; i++) {
			final String value = "foo" + i;
			attributes = Collections.singletonMap("foo", value);
			assertEquals("foo" + i, simple.getName());
		}

	}

	@Test
	public void testMultipleNestedValueFromProperties() throws Exception {

		for (int i = 0; i < 4; i++) {
			final String value = "bar" + i;
			attributes = Collections.singletonMap("foo", value);
			assertEquals("foo-bar" + i, nested.getName());
		}

	}

	@Test
	public void testMultipleValueInList() throws Exception {

		for (int i = 0; i < 4; i++) {
			final String value = "foo" + i;
			contextFactory.setContext(this);
			attributes = Collections.singletonMap("foo", value);
			try {
				assertEquals("foo" + i, list.getNames().get(0));
			}
			finally {
				contextFactory.clearContext();
			}
		}

	}

	@Test
	public void testMultipleValueInNestedList() throws Exception {

		for (int i = 0; i < 4; i++) {
			final String value = "foo" + i;
			contextFactory.setContext(this);
			attributes = Collections.singletonMap("foo", value);
			try {
				assertEquals("foo" + i, nestedList.getParent().getNames().get(0));
			}
			finally {
				contextFactory.clearContext();
			}
		}

	}

	@Test
	public void testMultipleValueInMap() throws Exception {

		for (int i = 0; i < 4; i++) {
			final String value = "foo" + i;
			contextFactory.setContext(this);
			attributes = Collections.singletonMap("foo", value);
			try {
				assertEquals("foo" + i, map.getMap().get("foo"));
			}
			finally {
				contextFactory.clearContext();
			}
		}

	}

	@Override
	public String toString() {
		return "Test context: attributes=" + attributes;
	}

	public static class TestBean {
		private String name;
		
		private TestBean parent;

		private List<String> names = new ArrayList<String>();

		private Map<String, String> map = new HashMap<String, String>();
		
		public TestBean getParent() {
			return parent;
		}

		public void setParent(TestBean parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getNames() {
			return new ArrayList<String>(names);
		}

		public void setNames(List<String> names) {
			this.names.addAll(names);
		}

		public Map<String, String> getMap() {
			return new HashMap<String, String>(map);
		}

		public void setMap(Map<String, String> map) {
			this.map.putAll(map);
		}
		
	}

	public static class SimpleContextFactory extends ContextFactorySupport {

		private Object root;

		public Object getContext() {
			return root;
		}

		public void setContext(Object root) {
			this.root = root;
		}

		public void clearContext() {
			root = null;
		}

	}

}
