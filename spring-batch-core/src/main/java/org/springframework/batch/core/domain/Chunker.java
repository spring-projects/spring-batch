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

import org.springframework.batch.io.exception.ReadFailureException;


/**
 * Interface defining the contract for reading a chunk. This is most useful when
 * implementing a 'chunk-oriented' approach to processing. Implementors of this
 * class are expected to aggregate the output of an ItemReader into 'chunks'.
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public interface Chunker {

	/**
	 * Read in a chunk, given the provided chunk size for the given StepExecution.
	 * 
	 * @param chunkSize the number of items that should be read for this chunk.
	 * @param StepExecution the stepExecution the current chunk is being processed within.
	 * @return the {@link Chunk} that has been read.
	 * @throws IllegalArgumentException if chunkSize is less than zero.
	 */
	public ChunkingResult chunk(int chunkSize, StepExecution stepExecution) throws ReadFailureException;
	
	/**
	 * Flush any chunks that may be buffered.  Because it may be advantageous to buffer chunks in case of
	 * failures, calling this method should flush any that are stored, however, it is not a requirement
	 * of the interface that it be implementated.
	 * 
	 * @param stepExecution
	 */
	public void flush(StepExecution stepExecution);

}
