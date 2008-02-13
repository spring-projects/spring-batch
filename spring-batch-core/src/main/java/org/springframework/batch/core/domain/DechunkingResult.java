/*
 * Copyright 2006-2007 the original author or authors.
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

/**
 * Value object representing the result of dechunking a {@link Chunk}.  It contains
 * the id of the chunk that was processed, a list of exceptions, and whether or not
 * the chunk was successful.
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class DechunkingResult {

	private final Long chunkId;

	private final List exceptions;
	
	private final boolean successful;

	public DechunkingResult(boolean successful, Long chunkId, List exceptions) {
		this.chunkId = chunkId;
		this.exceptions = exceptions;
		this.successful = successful;
	}

	public Long getChunkId() {
		return chunkId;
	}

	public List getExceptions() {
		return new ArrayList(exceptions);
	}
	
	public boolean isSuccessful() {
		return successful;
	}
}
