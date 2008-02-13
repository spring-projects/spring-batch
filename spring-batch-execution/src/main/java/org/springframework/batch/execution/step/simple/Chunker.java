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

import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.ChunkingResult;
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
	 * Read in a chunk, given the provided chunk size.
	 * 
	 * @param chunkSize the number of items that should be read for this chunk.
	 * @return the {@link Chunk} that has been read.
	 * @throws IllegalArgumentException if chunkSize is less than zero.
	 */
	public ChunkingResult chunk(int chunkSize) throws ReadFailureException;

}
