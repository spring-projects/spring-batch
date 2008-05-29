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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;

/**
 * @author Dave Syer
 * 
 */
class OrderedComposite {

	private List unordered = new ArrayList();

	private Collection ordered = new TreeSet(new OrderComparator());

	private List list = new ArrayList();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param items
	 */
	public void setItems(Object[] items) {
		unordered.clear();
		ordered.clear();
		for (int i = 0; i < items.length; i++) {
			add(items[i]);
		}
	}

	/**
	 * Register additional item.
	 * 
	 * @param item
	 */
	public void add(Object item) {
		if (item instanceof Ordered) {
			if (!ordered.contains(item)) {
				ordered.add(item);
			}
		}
		else {
			if (!unordered.contains(item)) {
				unordered.add(item);
			}
		}
		list.clear();
		list.addAll(ordered);
		list.addAll(unordered);
	}

	/**
	 * Public getter for the list of items. The {@link Ordered} items come
	 * first, followed by any unordered ones.
	 * @return an iterator over the list of items
	 */
	public Iterator iterator() {
		return new ArrayList(list).iterator();
	}

	/**
	 * Public getter for the list of items in reverse. The {@link Ordered} items come
	 * last, after any unordered ones.
	 * @return an iterator over the list of items
	 */
	public Iterator reverse() {
		ArrayList result = new ArrayList(list);
		Collections.reverse(result);
		return result.iterator();
	}

}
