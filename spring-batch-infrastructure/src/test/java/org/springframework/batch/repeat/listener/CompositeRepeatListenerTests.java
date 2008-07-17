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
package org.springframework.batch.repeat.listener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * @author Dave Syer
 * 
 */
public class CompositeRepeatListenerTests extends TestCase {

	private CompositeRepeatListener listener = new CompositeRepeatListener();
	private RepeatContext context = new RepeatContextSupport(null);

	private List<Object> list = new ArrayList<Object>();

	/**
	 * Test method for {@link CompositeRepeatListener#setListeners(RepeatListener[])}.
	 */
	public void testSetListeners() {
		listener.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				list.add("fail");
			}
		}, new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				list.add("continue");
			}
		} });
		listener.open(context);
		assertEquals(2, list.size());
	}

	/**
	 * Test method for
	 * {@link CompositeRepeatListener#register(RepeatListener)}.
	 */
	public void testSetListener() {
		listener.register(new RepeatListenerSupport() {
			public void before(RepeatContext context) {
				list.add("fail");
			}
		});
		listener.before(context);
		assertEquals(1, list.size());
	}

	public void testClose() {
		listener.register(new RepeatListenerSupport() {
			public void close(RepeatContext context) {
				list.add("foo");
			}
		});
		listener.close(context);
		assertEquals(1, list.size());
	}

	public void testOnError() {
		listener.register(new RepeatListenerSupport() {
			public void onError(RepeatContext context, Throwable e) {
				list.add(e);
			}
		});
		listener.onError(context, new RuntimeException("foo"));
		assertEquals(1, list.size());
	}

}
