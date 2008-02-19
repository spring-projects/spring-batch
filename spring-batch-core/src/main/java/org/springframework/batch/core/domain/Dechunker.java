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

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;

/**
 * Interface defining the contract for dechunking a {@link Chunk}.  Similar
 * to the differences between an {@link ItemReader} and an {@link ItemWriter},
 * a dechunker is the polar oposite of a {@link Chunker}.  A {@link Chunker} creates
 * a chunk from a stream of items, wheras a Dechunker removes each item from the 
 * {@link Chunk} and adds it to an outgoing stream of items.
 * 
 * @author Lucas Ward
 */
public interface Dechunker extends ItemStream{

	/**
	 * Dechunk the provided chunk.  In general, this will be done by delegating to an
	 * {@link ItemWriter} for each item in the {@link Chunk}, however, it's purely at the
	 * discretion of various implementations.
	 * 
	 * @param chunk to be 'dechunked'
	 * @param stepExecution the chunk belongs to.
	 * @return a Dechunking result detailing whether or not dechunking was successful, and
	 * if any items were skipped.
	 * @throws Exception, specifically throws IllegalArgumentException if either the chunk
	 * or StepExecution is null.
	 */
	DechunkingResult dechunk(Chunk chunk, StepExecution stepExecution) throws Exception;
}
