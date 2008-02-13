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
import org.springframework.batch.item.ItemWriter;
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
	private final StepExecution stepExecution;
	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();
		
	public ItemDechunker(ItemWriter itemWriter, StepExecution stepExecution) {
		this.itemWriter = itemWriter;
		this.stepExecution = stepExecution;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.Dechunker#dechunk(org.springframework.batch.core.domain.Chunk)
	 */
	public DechunkingResult dechunk(Chunk chunk) throws Exception {
		
		Assert.notNull(chunk, "Chunk must not be null");
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
	
}
