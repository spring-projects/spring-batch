/*
 * Copyright 2006-2013 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * @author Lucas Ward
 * @author Michael Minella
 *
 */
public class CompositeChunkListenerTests {

	ChunkListener listener;
	CompositeChunkListener compositeListener;
	ChunkContext chunkContext;

	@Before
	public void setUp() throws Exception {
		chunkContext = new ChunkContext(null);
		listener = createMock(ChunkListener.class);
		compositeListener = new CompositeChunkListener();
		compositeListener.register(listener);
	}

	@Test
	public void testBeforeChunk(){
		listener.beforeChunk(chunkContext);
		replay(listener);
		compositeListener.beforeChunk(chunkContext);
		verify(listener);
	}

	@Test
	public void testAfterChunk(){

		listener.afterChunk(chunkContext);
		replay(listener);
		compositeListener.afterChunk(chunkContext);
		verify(listener);
	}

	@Test
	public void testAfterChunkFailed(){
		ChunkContext context = new ChunkContext(null);
		listener.afterChunkError(context);
		replay(listener);
		compositeListener.afterChunkError(context);
		verify(listener);
	}
}
