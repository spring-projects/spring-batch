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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A 'chunk' of items, that will be committed together. It is expected that a
 * chunk may be serialized, especially if using a queue to dispatch chunks to
 * various {@link ChunkProcessor}s.</p>
 * 
 * <p>A note on serialization: A chunk is only as serializable as as the items
 * it contains, and no validation is performed to ensure they are serialiable.  
 * Therefore, it is expected that constructors of this class will ensure this 
 * condition if they intend to use the chunk in such a way that will require 
 * serialization.</p>
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class Chunk implements Serializable {

	private final Long id;

	private final List items;

	public Chunk(Long id, List items) {
		this.items = items;
		this.id = id;
	}

	/**
	 * Get the list of items for this chunk.
	 * 
	 * @return items.
	 */
	public List getItems() {
		return new ArrayList(items);
	}

	/**
	 * Get the chunk id.
	 * 
	 * @return id of this chunk.
	 */
	public Long getId() {
		return id;
	}
}
