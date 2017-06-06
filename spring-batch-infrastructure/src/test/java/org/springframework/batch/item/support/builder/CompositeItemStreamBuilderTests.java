/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.support.builder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.support.CompositeItemStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Glenn Renfro
 */
public class CompositeItemStreamBuilderTests {

	private List<String> list = new ArrayList<String>();

	@Test
	public void testRegisterAndOpen() {
		CompositeItemStream compositeStream = new CompositeItemStreamBuilder().build();
		ItemStreamSupport stream = new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				list.add("bar");
			}
		};
		compositeStream.register(stream);
		compositeStream.open(null);
		assertEquals(1, list.size());
	}

	@Test
	public void testCloseDoesNotUnregister() {
		ItemStream[] streams = new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				list.add("bar");
			}
		} };
		CompositeItemStream compositeStream = new CompositeItemStreamBuilder().streams(streams).build();

		compositeStream.open(null);
		compositeStream.close();
		compositeStream.open(null);
		assertEquals(2, list.size());
	}

	@Test
	public void testSetNullStreams() {
		try {
			new CompositeItemStreamBuilder().streams(null).build();
			fail("Expected an IllegalArgumentException.");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("Message returned from exception did not match expected result.", "Streams must not be null.",
					iae.getMessage());
		}
	}
}
