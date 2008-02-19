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
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.DechunkingResult;
import org.springframework.batch.core.domain.Dechunker;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.io.exception.WriteFailureException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link Dechunker} interface that passes items to an
 * {@link ItemWriter} one at a time.
 * 
 * @author Lucas Ward
 *
 */
public class ItemDechunker implements Dechunker {

	private final ItemWriter itemWriter;
	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();
		
	public ItemDechunker(ItemWriter itemWriter) {
		this.itemWriter = itemWriter;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.Dechunker#dechunk(org.springframework.batch.core.domain.Chunk)
	 */
	public DechunkingResult dechunk(Chunk chunk, StepExecution stepExecution) throws Exception {
		
		Assert.notNull(chunk, "Chunk must not be null");
		Assert.notNull(stepExecution, "StepExecution must not be null");
		List skippedItems = new ArrayList();
		for(Iterator it = chunk.getItems().iterator(); it.hasNext();){
			
			Object item = it.next();
			try{
				itemWriter.write(item);
			}
			catch(Exception ex){
				if(itemSkipPolicy.shouldSkip(ex, stepExecution)){
					stepExecution.incrementSkipCount();
					skippedItems.add(new WriteFailureException(ex, item));
				}
				else{
					rethrow(ex);
				}
			}
		}
		
		return new DechunkingResult(true, chunk.getId(), skippedItems);
	}
	
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}
	
	private void rethrow(Exception ex){
		if(ex instanceof RuntimeException){
			throw (RuntimeException)ex;
		}
		else{
			throw new RuntimeException("Error encountered while dechunking", ex);
		}
	}

	//This a hack until the ItemStream interface can be updated.
	public void close() throws StreamException {
		if(itemWriter instanceof ItemStream){
			((ItemStream)itemWriter).close();
		}
	}

	public boolean isMarkSupported() {
		if(itemWriter instanceof ItemStream){
			return ((ItemStream)itemWriter).isMarkSupported();
		}
		else{
			return false;
		}
	}

	public void mark() throws MarkFailedException {
		if(itemWriter instanceof ItemStream){
			((ItemStream)itemWriter).mark();
		}
	}

	public void open() throws StreamException {
		if(itemWriter instanceof ItemStream){
			((ItemStream)itemWriter).open();
		}
	}

	public void reset() throws ResetFailedException {
		if(itemWriter instanceof ItemStream){
			((ItemStream)itemWriter).reset();
		}
	}

	public void restoreFrom(ExecutionContext context) {
		if(itemWriter instanceof ItemStream){
			((ItemStream)itemWriter).restoreFrom(context);
		}
	}

	public ExecutionContext getExecutionContext() {
		if(itemWriter instanceof ItemStream){
			return ((ItemStream)itemWriter).getExecutionContext();
		}
		else{
			return new ExecutionContext();
		}
	}
	
}
