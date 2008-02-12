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

/**
 * Interface defining the contract for dechunking a {@link Chunk}.  Similar
 * to the differences between an {@link ItemReader} and an {@link ItemWriter},
 * a dechunker is the polar oposite of a {@link Chunker}.  A {@link Chunker} creates
 * a chunk from a stream of items, wheras a Dechunker removes each item from the 
 * {@link Chunk} and adds it to an outgoing stream of items.
 * 
 * @author Lucas Ward
 *
 */
public interface Dechunker {

	ChunkResult dechunk(Chunk chunk) throws Exception;
}
