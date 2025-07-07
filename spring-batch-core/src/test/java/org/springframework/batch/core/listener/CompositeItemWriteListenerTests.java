/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;

import static org.mockito.Mockito.mock;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class CompositeItemWriteListenerTests {

	ItemWriteListener<Object> listener;

	CompositeItemWriteListener<Object> compositeListener;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
		listener = mock();
		compositeListener = new CompositeItemWriteListener<>();
		compositeListener.register(listener);
	}

	@Test
	void testBeforeWrite() {
		Chunk<Object> item = Chunk.of(new Object());
		listener.beforeWrite(item);
		compositeListener.beforeWrite(item);
	}

	@Test
	void testAfterWrite() {
		Chunk<Object> item = Chunk.of(new Object());
		listener.afterWrite(item);
		compositeListener.afterWrite(item);
	}

	@Test
	void testOnWriteError() {
		Chunk<Object> item = Chunk.of(new Object());
		Exception ex = new Exception();
		listener.onWriteError(ex, item);
		compositeListener.onWriteError(ex, item);
	}

	@Test
	void testSetListeners() {
		compositeListener.setListeners(List.of(listener));
		Chunk<Object> item = Chunk.of(new Object());
		listener.beforeWrite(item);
		compositeListener.beforeWrite(item);
	}

}
