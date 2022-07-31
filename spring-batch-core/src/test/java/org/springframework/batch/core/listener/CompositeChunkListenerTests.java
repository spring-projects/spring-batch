/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * @author Lucas Ward
 * @author Michael Minella
 * @author Will Schipp
 *
 */
class CompositeChunkListenerTests {

	ChunkListener listener;

	CompositeChunkListener compositeListener;

	ChunkContext chunkContext;

	@BeforeEach
	void setUp() {
		chunkContext = new ChunkContext(null);
		listener = mock(ChunkListener.class);
		compositeListener = new CompositeChunkListener();
		compositeListener.register(listener);
	}

	@Test
	void testBeforeChunk() {
		listener.beforeChunk(chunkContext);
		compositeListener.beforeChunk(chunkContext);
	}

	@Test
	void testAfterChunk() {
		listener.afterChunk(chunkContext);
		compositeListener.afterChunk(chunkContext);
	}

	@Test
	void testAfterChunkFailed() {
		ChunkContext context = new ChunkContext(null);
		listener.afterChunkError(context);
		compositeListener.afterChunkError(context);
	}

}
