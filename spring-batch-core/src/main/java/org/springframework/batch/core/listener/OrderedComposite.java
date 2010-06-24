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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

/**
 * @author Dave Syer
 * 
 */
class OrderedComposite<S> {

	private List<S> unordered = new ArrayList<S>();

	private List<S> ordered = new ArrayList<S>();
	
	@SuppressWarnings("unchecked")
	private Comparator<? super S> comparator = new AnnotationAwareOrderComparator();

	private List<S> list = new ArrayList<S>();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param items
	 */
	public void setItems(List<? extends S> items) {
		unordered.clear();
		ordered.clear();
		for (S s : items) {
			add(s);
		}
	}

	/**
	 * Register additional item.
	 * 
	 * @param item
	 */
	public void add(S item) {
		if (item instanceof Ordered) {
			if (!ordered.contains(item)) {
				ordered.add(item);
			}
		}
		else if (AnnotationUtils.isAnnotationDeclaredLocally(Order.class, item.getClass())) {
			if (!ordered.contains(item)) {
				ordered.add(item);
			}
		}
		else if (!unordered.contains(item)) {
			unordered.add(item);
		}
		Collections.sort(ordered, comparator);
		list.clear();
		list.addAll(ordered);
		list.addAll(unordered);
	}

	/**
	 * Public getter for the list of items. The {@link Ordered} items come
	 * first, followed by any unordered ones.
	 * @return an iterator over the list of items
	 */
	public Iterator<S> iterator() {
		return new ArrayList<S>(list).iterator();
	}

	/**
	 * Public getter for the list of items in reverse. The {@link Ordered} items
	 * come last, after any unordered ones.
	 * @return an iterator over the list of items
	 */
	public Iterator<S> reverse() {
		ArrayList<S> result = new ArrayList<S>(list);
		Collections.reverse(result);
		return result.iterator();
	}

}
