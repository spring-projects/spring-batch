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
package org.springframework.batch.execution.step.support;

import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.DechunkingResult;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.execution.step.support.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.execution.step.support.ItemDechunker;
import org.springframework.batch.io.exception.WriteFailureException;
import org.springframework.batch.item.ItemWriter;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class ItemDechunkerTests extends TestCase {

	private ItemDechunker dechunker;
	private StepContribution stepContribution;
	private Chunk chunk;
	private ItemWriter itemWriter;
	private MockControl writerControl = MockControl.createControl(ItemWriter.class);
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		itemWriter = (ItemWriter)writerControl.getMock();
		StepExecution execution = new StepExecution(null,null);
		stepContribution = execution.createStepContribution();
		dechunker = new ItemDechunker(itemWriter);
		List items = new ArrayList();
		items.add("1");
		items.add("2");
		chunk = new Chunk(new Long(1),items);
	}
	
	
	public void testNormalProcessing() throws Exception{
		
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.replay();
		dechunker.dechunk(chunk, stepContribution);
		writerControl.verify();
	}
	
	public void testSkip() throws Exception{
		
		dechunker.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.setThrowable(new Exception());
		writerControl.replay();
		DechunkingResult result = dechunker.dechunk(chunk, stepContribution);
		writerControl.verify();
		List exceptions = result.getExceptions();
		assertEquals(1, exceptions.size());
		WriteFailureException exception = (WriteFailureException)exceptions.get(0);
		assertEquals("2",exception.getItem());
		
	}
	
	public void testFailure() throws Exception{
		itemWriter.write("1");
		itemWriter.write("2");
		writerControl.setThrowable(new NullPointerException());
		writerControl.replay();
		try{
			dechunker.dechunk(chunk, stepContribution);
			fail();
		}
		catch(NullPointerException ex){
			//expected
		}
	}
}
