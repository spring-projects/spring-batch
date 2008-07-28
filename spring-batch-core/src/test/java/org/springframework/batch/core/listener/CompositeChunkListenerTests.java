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
package org.springframework.batch.core.listener;

import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.listener.CompositeChunkListener;

/**
 * @author Lucas Ward
 *
 */
public class CompositeChunkListenerTests {

	ChunkListener listener;
	CompositeChunkListener compositeListener;
	
	@Before
	public void setUp() throws Exception {
		listener = createMock(ChunkListener.class);
		compositeListener = new CompositeChunkListener();
		compositeListener.register(listener);
	}
	
	@Test
	public void testBeforeChunk(){
		
		listener.beforeChunk();
		replay(listener);
		compositeListener.beforeChunk();
		verify(listener);
	}
	
	@Test
	public void testAfterChunk(){
		
		listener.afterChunk();
		replay(listener);
		compositeListener.afterChunk();
		verify(listener);
	}
}
