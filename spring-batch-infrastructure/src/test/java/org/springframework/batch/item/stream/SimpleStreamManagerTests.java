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
package org.springframework.batch.item.stream;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStreamManagerTests extends TestCase {

	private CompositeItemStream manager = new CompositeItemStream();

	private List list = new ArrayList();

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.stream.CompositeItemStream#commit(org.springframework.transaction.TransactionStatus)}.
	 */
	public void testRegisterAndOpen() {
		ItemStreamSupport stream = new ItemStreamSupport() {
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				list.add("bar");
			}
		};
		manager.register(stream);
		manager.open(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.stream.CompositeItemStream#commit(org.springframework.transaction.TransactionStatus)}.
	 */
	public void testRegisterTwice() {
		ItemStreamSupport stream = new ItemStreamSupport() {
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				list.add("bar");
			}
		};
		manager.register(stream);
		manager.register(stream);
		manager.open(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.stream.CompositeItemStream#commit(org.springframework.transaction.TransactionStatus)}.
	 */
	public void testMark() {
		manager.register(new ItemStreamSupport() {
			public void update(ExecutionContext executionContext) {
				list.add("bar");
			}
		});
		manager.update(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.stream.CompositeItemStream#commit(org.springframework.transaction.TransactionStatus)}.
	 */
	public void testClose() {
		manager.register(new ItemStreamSupport() {
			public void close(ExecutionContext executionContext) throws ItemStreamException {
				list.add("bar");
			}
		});
		manager.close(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.stream.CompositeItemStream#commit(org.springframework.transaction.TransactionStatus)}.
	 */
	public void testCloseDoesNotUnregister() {
		manager.setStreams(new ItemStream[] { new ItemStreamSupport() {
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				list.add("bar");
			}
		} });
		manager.open(null);
		manager.close(null);
		manager.open(null);
		assertEquals(2, list.size());
	}

}
