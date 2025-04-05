/*
 * Copyright 2006-2023 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ItemReadListener;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class CompositeItemReadListenerTests {

	ItemReadListener<Object> listener;

	CompositeItemReadListener<Object> compositeListener;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
		listener = mock();
		compositeListener = new CompositeItemReadListener<>();
		compositeListener.register(listener);
	}

	@Test
	void testBeforeRead() {

		listener.beforeRead();
		compositeListener.beforeRead();
	}

	@Test
	void testAfterRead() {
		Object item = new Object();
		listener.afterRead(item);
		compositeListener.afterRead(item);
	}

	@Test
	void testOnReadError() {

		Exception ex = new Exception();
		listener.onReadError(ex);
		compositeListener.onReadError(ex);
	}

	@Test
	void testSetListeners() {
		compositeListener.setListeners(List.of(listener));
		listener.beforeRead();
		compositeListener.beforeRead();
	}

}
