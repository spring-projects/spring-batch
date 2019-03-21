/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.listener;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * Basic support implementation of {@link ChunkListener}
 *
 * @author Lucas Ward
 * @author Michael Minella
 *
 */
public class ChunkListenerSupport implements ChunkListener {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#afterChunk()
	 */
	@Override
	public void afterChunk(ChunkContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#beforeChunk()
	 */
	@Override
	public void beforeChunk(ChunkContext context) {
	}


	@Override
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#afterChunkError(ChunkContext)
	 */
	public void afterChunkError(ChunkContext context) {
	}

}
