/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.domain;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class ChunkTests extends TestCase {

	List items;
	Chunk chunk;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		items = new ArrayList();
		items.add("1");
		items.add("2");
	}
	
	
	public void testCreateChunk(){
		
		chunk = new Chunk(new Long(1), items);
		assertEquals(items, chunk.getItems());
	}
	
	public void testImmutability(){
		
		chunk = new Chunk(new Long(1), items);
		List testItems = chunk.getItems();	
		testItems.add("3");
		assertEquals(2, chunk.getItems().size());
	}
}
