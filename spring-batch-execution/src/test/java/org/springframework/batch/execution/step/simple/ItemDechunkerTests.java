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
package org.springframework.batch.execution.step.simple;

import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.ChunkResult;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.item.ItemWriter;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class ItemDechunkerTests extends TestCase {

	private ItemDechunker dechunker;
	private StepExecution stepExecution;
	private Chunk chunk;
	private ItemWriter itemWriter;
	private MockControl writerControl = MockControl.createControl(ItemWriter.class);
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		itemWriter = (ItemWriter)writerControl.getMock();
		stepExecution = new StepExecution(null,null);
		dechunker = new ItemDechunker(itemWriter, stepExecution);
		List items = new ArrayList();
		items.add("1");
		items.add("2");
		chunk = new Chunk(new Long(1),items);
	}
	
	
	public void testNormalProcessing() throws Exception{
		
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.replay();
		dechunker.dechunk(chunk);
		writerControl.verify();
	}
	
	public void testSkip() throws Exception{
		
		dechunker.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.setThrowable(new Exception());
		writerControl.replay();
		ChunkResult result = dechunker.dechunk(chunk);
		writerControl.verify();
		assertEquals("2",result.getSkippedItems().get(0));
		
	}
	
	public void testFailure() throws Exception{
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.setThrowable(new NullPointerException());
		writerControl.replay();
		try{
			dechunker.dechunk(chunk);
			fail();
		}
		catch(NullPointerException ex){
			//expected
		}
	}
}
