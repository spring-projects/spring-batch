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
package org.springframework.batch.core.listener;

import java.util.Iterator;

import org.springframework.core.Ordered;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class OrderedCompositeTests extends TestCase {
	
	private OrderedComposite list = new OrderedComposite();

	/**
	 * Test method for {@link org.springframework.batch.core.listener.OrderedComposite#setItems(java.lang.Object[])}.
	 */
	public void testSetItems() {
		list.setItems(new String[] {"1", "2"});
		Iterator<Object> iterator = list.iterator();
		assertEquals("1", iterator.next());
		assertEquals("2", iterator.next());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.listener.OrderedComposite#add(java.lang.Object)}.
	 */
	public void testAdd() {
		list.setItems(new String[] {"1"});
		list.add("3");
		Iterator<Object> iterator = list.iterator();
		assertEquals("1", iterator.next());
		assertEquals("3", iterator.next());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.listener.OrderedComposite#add(java.lang.Object)}.
	 */
	public void testAddOrdered() {
		list.setItems(new String[] {"1"});
		list.add(new Ordered() {
			public int getOrder() {
				return 0;
			}
		});
		Iterator<Object> iterator = list.iterator();
		iterator.next();
		assertEquals("1", iterator.next());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.listener.OrderedComposite#add(java.lang.Object)}.
	 */
	public void testAddMultipleOrdered() {
		list.setItems(new String[] {"1"});
		list.add(new Ordered() {
			public int getOrder() {
				return 1;
			}
		});
		list.add(new Ordered() {
			public int getOrder() {
				return 0;
			}
		});
		Iterator<Object> iterator = list.iterator();
		assertEquals(0, ((Ordered) iterator.next()).getOrder());
		assertEquals(1, ((Ordered) iterator.next()).getOrder());
		assertEquals("1", iterator.next());
	}

}
